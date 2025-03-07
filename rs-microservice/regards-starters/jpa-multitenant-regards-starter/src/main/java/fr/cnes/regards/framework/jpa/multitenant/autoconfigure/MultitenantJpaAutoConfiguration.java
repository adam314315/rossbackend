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
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.exception.MultiDataBasesException;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.resolver.CurrentTenantIdentifierResolverImpl;
import fr.cnes.regards.framework.jpa.multitenant.resolver.DataSourceBasedMultiTenantConnectionProviderImpl;
import fr.cnes.regards.framework.jpa.utils.DaoUtils;
import fr.cnes.regards.framework.jpa.utils.IDatasourceSchemaHelper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import jakarta.persistence.Entity;

/**
 * Configuration class to define hibernate/jpa multitenancy databases strategy
 *
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 */
@AutoConfiguration(after = { GsonAutoConfiguration.class, AmqpAutoConfiguration.class })
@EnableJpaRepositories(excludeFilters = { @ComponentScan.Filter(value = InstanceEntity.class,
                                                                type = FilterType.ANNOTATION) },
                       basePackages = DaoUtils.ROOT_PACKAGE,
                       entityManagerFactoryRef = "multitenantsEntityManagerFactory",
                       transactionManagerRef = MultitenantDaoProperties.MULTITENANT_TRANSACTION_MANAGER)
@EnableTransactionManagement
@EnableConfigurationProperties({ JpaProperties.class })
@ConditionalOnProperty(prefix = "regards.jpa", name = "multitenant.enabled", matchIfMissing = true)
public class MultitenantJpaAutoConfiguration {

    /**
     * JPA Persistence unit name. Used to separate multiples databases.
     */
    public static final String PERSITENCE_UNIT_NAME = "multitenant";

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Data sources pool
     */
    @Autowired
    @Qualifier(DataSourcesAutoConfiguration.DATA_SOURCE_BEAN_NAME)
    private Map<String, DataSource> dataSources;

    @Autowired
    @Qualifier(DataSourcesAutoConfiguration.DATASOURCE_SCHEMA_HELPER_BEAN_NAME)
    private IDatasourceSchemaHelper datasourceSchemaHelper;

    /**
     * Transaction manager builder
     */
    @Autowired
    private EntityManagerFactoryBuilder builder;

    /**
     * Constructor. Check for classpath errors.
     */
    public MultitenantJpaAutoConfiguration() throws MultiDataBasesException {
        DaoUtils.checkClassPath(DaoUtils.ROOT_PACKAGE);
    }

    /**
     * Create the tenant resolver. Select the tenant when a connection is needed.
     *
     * @return {@link CurrentTenantIdentifierResolverImpl}
     */
    @Bean
    public CurrentTenantIdentifierResolver currentTenantIdentifierResolver(IRuntimeTenantResolver threadTenantResolver) {
        return new CurrentTenantIdentifierResolverImpl(threadTenantResolver);
    }

    /**
     * Create the connection provider. Used to select datasource for a given tenant
     *
     * @return {@link DataSourceBasedMultiTenantConnectionProviderImpl}
     */
    @Bean
    public AbstractDataSourceBasedMultiTenantConnectionProviderImpl connectionProvider() {
        return new DataSourceBasedMultiTenantConnectionProviderImpl(dataSources);
    }

    /**
     * Create Transaction manager for multitenancy projects datasources
     *
     * @return {@link PlatformTransactionManager}
     */
    @Bean(name = MultitenantDaoProperties.MULTITENANT_TRANSACTION_MANAGER)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Primary
    public PlatformTransactionManager multitenantsJpaTransactionManager(MultiTenantConnectionProvider multiTenantConnectionProvider,
                                                                        CurrentTenantIdentifierResolver currentTenantIdentifierResolver)
        throws JpaMultitenantException {
        final JpaTransactionManager jtm = new JpaTransactionManager();
        jtm.setEntityManagerFactory(multitenantsEntityManagerFactory(multiTenantConnectionProvider,
                                                                     currentTenantIdentifierResolver).getObject());
        return jtm;
    }

    /**
     * Create EntityManagerFactory for multitenancy datasources
     *
     * @return {@link LocalContainerEntityManagerFactoryBean}
     */
    @Bean(name = "multitenantsEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean multitenantsEntityManagerFactory(MultiTenantConnectionProvider multiTenantConnectionProvider,
                                                                                   CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
        // Use the first dataSource configuration to init the entityManagerFactory
        if (dataSources.isEmpty()) {
            throw new ApplicationContextException("No datasource defined. JPA is not able to start."
                                                  + " You should define a datasource in the application.properties of the current microservice");
        }
        final DataSource defaultDataSource = dataSources.values().iterator().next();

        // Init with common properties
        final Map<String, Object> hibernateProps = datasourceSchemaHelper.getHibernateProperties();

        // Add multitenant properties
        hibernateProps.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
        hibernateProps.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);

        // Find classpath for each Entity and not NonStandardEntity
        final Set<String> packagesToScan = DaoUtils.findPackagesForJpa(DaoUtils.ROOT_PACKAGE);
        final List<Class<?>> packages = DaoUtils.scanPackagesForJpa(Entity.class, InstanceEntity.class, packagesToScan);

        return builder.dataSource(defaultDataSource)
                      .persistenceUnit(PERSITENCE_UNIT_NAME)
                      .packages(packages.toArray(new Class[0]))
                      .properties(hibernateProps)
                      .jta(false)
                      .build();
    }

    /**
     * this bean allow us to set <b>our</b> instance of Gson, customized for the serialization of any data as jsonb into
     * the database
     */
    @Bean
    public Void setGsonIntoGsonUtil(@Qualifier("gson") Gson pGson) {
        GsonUtil.setGson(pGson);
        return null;
    }
}
