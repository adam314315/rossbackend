/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.jpa.multitenant.event;

import com.zaxxer.hikari.HikariDataSource;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockService;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.jpa.multitenant.utils.TenantDataSourceHelper;
import fr.cnes.regards.framework.jpa.utils.IDatasourceSchemaHelper;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * This class manages JPA event workflow.
 *
 * @author Marc Sordi
 */
public class MultitenantJpaEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultitenantJpaEventHandler.class);

    /**
     * Current microservice name
     */
    private final String microserviceName;

    /**
     * AMQP Message subscriber
     */
    private final IInstanceSubscriber instanceSubscriber;

    /**
     * Spring events for local events broadcasting
     */
    private final MultitenantJpaEventPublisher localPublisher;

    /**
     * Custom projects dao connection reader
     */
    private final ITenantConnectionResolver multitenantResolver;

    /**
     * Pool of datasources available for this connection provider
     */
    private final Map<String, DataSource> dataSources;

    private final ILockingTaskExecutors lockingTaskExecutors;

    /**
     * Microservice global configuration
     */
    private final MultitenantDaoProperties daoProperties;

    /**
     * Schema update helper
     */
    private final IDatasourceSchemaHelper datasourceSchemaHelper;

    private final IEncryptionService encryptionService;

    /**
     * JPA Configuration
     */
    private final JpaProperties jpaProperties;

    private final LockService lockService;

    public MultitenantJpaEventHandler(String microserviceName,
                                      Map<String, DataSource> dataSources,
                                      ILockingTaskExecutors lockingTaskExecutors,
                                      MultitenantDaoProperties daoProperties,
                                      IDatasourceSchemaHelper datasourceSchemaHelper,
                                      IInstanceSubscriber instanceSubscriber,
                                      ITenantConnectionResolver multitenantResolver,
                                      MultitenantJpaEventPublisher localPublisher,
                                      IEncryptionService encryptionService,
                                      JpaProperties jpaProperties,
                                      LockService lockService) {
        this.microserviceName = microserviceName;
        this.dataSources = dataSources;
        this.lockingTaskExecutors = lockingTaskExecutors;
        this.daoProperties = daoProperties;
        this.datasourceSchemaHelper = datasourceSchemaHelper;
        this.instanceSubscriber = instanceSubscriber;
        this.multitenantResolver = multitenantResolver;
        this.localPublisher = localPublisher;
        this.encryptionService = encryptionService;
        this.jpaProperties = jpaProperties;
        this.lockService = lockService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Listen to tenant connection creation
        instanceSubscriber.subscribeTo(TenantConnectionConfigurationCreated.class, new ConfigurationCreatedHandler());
        // Listen to tenant connection update
        instanceSubscriber.subscribeTo(TenantConnectionConfigurationUpdated.class, new ConfigurationUpdatedHandler());
        // Listen to tenant connection deletion
        instanceSubscriber.subscribeTo(TenantConnectionConfigurationDeleted.class, new ConfigurationDeletedHandler());
        // Listen to tenant connection failure
        instanceSubscriber.subscribeTo(TenantConnectionFailed.class, new TenantConnectionFailedHandler());
    }

    /**
     * Handle a new {@link TenantConnection} if the related microservice is exactly the same.
     *
     * @param eventMicroserviceName related microservice
     * @param tenantConnection      {@link TenantConnection} for the microservice
     */
    private void handleTenantConnection(String eventMicroserviceName, TenantConnection tenantConnection) {
        if (microserviceName.equals(eventMicroserviceName)) {
            String tenant = tenantConnection.getTenant();
            try {
                // Trying to connect data source
                updateConnectionState(tenant, TenantConnectionState.CONNECTING, Optional.empty());

                // Retrieve schema name
                String schemaIdentifier = jpaProperties.getProperties().get(Environment.DEFAULT_SCHEMA);
                // Init data source
                // before initiating data source, lets decrypt password
                tenantConnection.setPassword(encryptionService.decrypt(tenantConnection.getPassword()));
                TenantDataSourceHelper.verifyBatchParameter(jpaProperties, tenantConnection);
                DataSource dataSource = TenantDataSourceHelper.initDataSource(daoProperties,
                                                                              tenantConnection,
                                                                              schemaIdentifier);
                // Remove existing one
                DataSource oldDataSource = dataSources.remove(tenant);
                // Remove related lock executor
                lockingTaskExecutors.removeLockingTaskExecutor(tenant);
                // Remove related lock registry
                lockService.removeLockRegistry(tenant);
                if (oldDataSource != null) {
                    oldDataSource.unwrap(HikariDataSource.class).close();
                }
                // Update schema
                datasourceSchemaHelper.migrate(dataSource, tenant);
                // Enable data source
                updateConnectionState(tenant, TenantConnectionState.ENABLED, Optional.empty());
                // Register data source
                dataSources.put(tenant, dataSource);
                // Register a lock executor
                lockingTaskExecutors.registerLockingTaskExecutor(tenant, dataSource);
                // Register a lock registry
                lockService.registerLockRegistry(tenant, dataSource);
                // Broadcast connection ready with a Spring event
                localPublisher.publishConnectionReady(tenant);
            } catch (Exception ex) {
                LOGGER.error(String.format("Cannot handle tenant connection for project %s and microservice %s",
                                           tenant,
                                           eventMicroserviceName), ex);
                updateConnectionState(tenant, TenantConnectionState.ERROR, Optional.ofNullable(ex.getMessage()));
            }
        }
    }

    private void updateConnectionState(String tenant, TenantConnectionState state, Optional<String> errorCause) {
        try {
            multitenantResolver.updateState(microserviceName, tenant, state, errorCause);
        } catch (JpaMultitenantException ex) {
            LOGGER.error(String.format("Cannot update datasource for tenant %s and microservice %s. Update fails.",
                                       tenant,
                                       microserviceName), ex);
        }
    }

    /**
     * Handle {@link TenantConnection} configuration creation
     *
     * @author Sébastien Binda
     */
    private class ConfigurationCreatedHandler implements IHandler<TenantConnectionConfigurationCreated> {

        @Override
        public void handle(final TenantWrapper<TenantConnectionConfigurationCreated> pEvent) {
            if (pEvent.getContent() != null) {
                handleTenantConnection(pEvent.getContent().getMicroserviceName(), pEvent.getContent().getTenant());
            }
        }
    }

    /**
     * Handle {@link TenantConnection} configuration update
     *
     * @author Marc Sordi
     */
    private class ConfigurationUpdatedHandler implements IHandler<TenantConnectionConfigurationUpdated> {

        @Override
        public void handle(TenantWrapper<TenantConnectionConfigurationUpdated> pEvent) {
            if (pEvent.getContent() != null) {
                handleTenantConnection(pEvent.getContent().getMicroserviceName(), pEvent.getContent().getTenant());
            }
        }
    }

    /**
     * Handle {@link TenantConnection} configuration deletion
     *
     * @author Marc Sordi
     */
    private class ConfigurationDeletedHandler implements IHandler<TenantConnectionConfigurationDeleted> {

        @Override
        public void handle(TenantWrapper<TenantConnectionConfigurationDeleted> pEvent) {

            if ((pEvent.getContent() != null) && microserviceName.equals(pEvent.getContent().getMicroserviceName())) {
                final TenantConnection tenantConnection = pEvent.getContent().getTenant();
                try {
                    // Remove existing datasource
                    DataSource oldDataSource = dataSources.remove(tenantConnection.getTenant());
                    // Remove related lock executor
                    lockingTaskExecutors.removeLockingTaskExecutor(tenantConnection.getTenant());
                    // Remove related lock registry
                    lockService.removeLockRegistry(tenantConnection.getTenant());
                    if (oldDataSource != null) {
                        oldDataSource.unwrap(HikariDataSource.class).close();
                    }
                    // Broadcast connection discarded with a Spring event
                    localPublisher.publishConnectionDiscarded(tenantConnection.getTenant());
                } catch (SQLException e) {
                    LOGGER.error(
                        "Cannot release datasource for tenant {}. Delete fails while closing existing connection.",
                        tenantConnection.getTenant());
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Handle {@link TenantConnection} fail event
     *
     * @author Marc Sordi
     */
    private class TenantConnectionFailedHandler implements IHandler<TenantConnectionFailed> {

        @Override
        public void handle(TenantWrapper<TenantConnectionFailed> pEvent) {

            if ((pEvent.getContent() != null) && microserviceName.equals(pEvent.getContent().getMicroserviceName())) {
                final TenantConnectionFailed tcf = pEvent.getContent();
                try {
                    // Remove existing datasource
                    DataSource oldDataSource = dataSources.remove(tcf.getTenant());
                    // Remove related lock executor
                    lockingTaskExecutors.removeLockingTaskExecutor(tcf.getTenant());
                    // Remove related lock registry
                    lockService.removeLockRegistry(tcf.getTenant());
                    if (oldDataSource != null) {
                        oldDataSource.unwrap(HikariDataSource.class).close();
                    }
                    // Disable connection
                    multitenantResolver.updateState(microserviceName,
                                                    tcf.getTenant(),
                                                    TenantConnectionState.ERROR,
                                                    Optional.of("Connection failed event received!"));
                } catch (SQLException e) {
                    LOGGER.error("Cannot release datasource for tenant {}. Cannot close connection", tcf.getTenant());
                    LOGGER.error(e.getMessage(), e);
                } catch (JpaMultitenantException e) {
                    LOGGER.error("Cannot disable datasource for tenant {}. Update fails.", tcf.getTenant());
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

}
