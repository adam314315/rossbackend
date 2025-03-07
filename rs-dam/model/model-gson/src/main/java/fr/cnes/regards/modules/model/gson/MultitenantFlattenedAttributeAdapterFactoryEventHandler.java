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
package fr.cnes.regards.modules.model.gson;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.domain.event.AttributeModelCreated;
import fr.cnes.regards.modules.model.domain.event.AttributeModelDeleted;
import fr.cnes.regards.modules.model.domain.event.FragmentDeletedEvent;
import fr.cnes.regards.modules.model.dto.event.AttributeCacheRefreshEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler to initialize subTypes for MultitenantFlattenedAttributeAdapterFactory after ApplicationReadyEvent sent.
 *
 * @author Sébastien Binda
 */
@Component
public class MultitenantFlattenedAttributeAdapterFactoryEventHandler
    implements ApplicationListener<ApplicationStartedEvent> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultitenantFlattenedAttributeAdapterFactoryEventHandler.class);

    /**
     * AMQ Subscriber
     */
    @Autowired
    private ISubscriber subscriber;

    /**
     * Tenant resolver
     */
    @Autowired
    private ITenantResolver tenantResolver;

    /**
     * Helper class to initialize factory based on stored {@link AttributeModel}
     */
    @Autowired
    private IAttributeHelper attributeHelper;

    /**
     * Factory to work with
     */
    @Autowired
    private MultitenantFlattenedAttributeAdapterFactory factory;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private INotificationClient notifClient;

    @Value("spring.application.name")
    private String microserviceName;

    @Override
    public void onApplicationEvent(final ApplicationStartedEvent pEvent) {
        subscriber.subscribeTo(AttributeModelCreated.class, new RegisterHandler());
        subscriber.subscribeTo(AttributeModelDeleted.class, new UnregisterHandler());
        subscriber.subscribeTo(FragmentDeletedEvent.class, new UnregisterFragmentHandler());
        subscriber.subscribeTo(AttributeCacheRefreshEvent.class, new AttributeCacheRefreshHandler());
        // Retrieve all tenants
        for (final String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                // Register for tenant
                LOGGER.info("Registering already configured attributes and fragments");
                final List<AttributeModel> atts = attributeHelper.getAllAttributes();
                // Use factory algorithm
                factory.registerAttributes(tenant, atts);
                LOGGER.info("Registering attributes for tenant {} done", tenant);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
        applicationEventPublisher.publishEvent(new ModelGsonReadyEvent(this));
    }

    /**
     * Handle {@link AttributeModel} creation
     *
     * @author Marc Sordi
     */
    private class RegisterHandler implements IHandler<AttributeModelCreated> {

        @Override
        public void handle(final TenantWrapper<AttributeModelCreated> pWrapper) {
            final AttributeModelCreated amc = pWrapper.getContent();

            String tenant = pWrapper.getTenant();
            Fragment fragment = new Fragment();
            fragment.setName(amc.getFragmentName());
            AttributeModel attributeModel = new AttributeModelBuilder(amc.getAttributeName(),
                                                                      amc.getPropertyType(),
                                                                      null).setFragment(fragment).build();

            // Use factory algorithm
            factory.registerAttribute(tenant, attributeModel);
        }
    }

    /**
     * Handle {@link AttributeModel} deletion
     *
     * @author Marc Sordi
     */
    private class UnregisterHandler implements IHandler<AttributeModelDeleted> {

        @Override
        public void handle(final TenantWrapper<AttributeModelDeleted> pWrapper) {
            AttributeModelDeleted amd = pWrapper.getContent();

            String tenant = pWrapper.getTenant();
            Fragment fragment = new Fragment();
            fragment.setName(amd.getFragmentName());
            AttributeModel attributeModel = new AttributeModelBuilder(amd.getAttributeName(),
                                                                      amd.getPropertyType(),
                                                                      null).setFragment(fragment).build();

            // Use factory algorithm
            factory.unregisterAttribute(tenant, attributeModel);
        }
    }

    /**
     * Handle {@link Fragment} deletion
     *
     * @author Marc Sordi
     */
    private class UnregisterFragmentHandler implements IHandler<FragmentDeletedEvent> {

        @Override
        public void handle(final TenantWrapper<FragmentDeletedEvent> pWrapper) {
            String tenant = pWrapper.getTenant();
            FragmentDeletedEvent fragmentDeleted = pWrapper.getContent();

            Fragment fragment = new Fragment();
            fragment.setName(fragmentDeleted.getFragmentName());

            // Use factory algorithm
            factory.unregisterFragment(tenant, fragment);
        }
    }

    private class AttributeCacheRefreshHandler implements IBatchHandler<AttributeCacheRefreshEvent> {

        @Override
        public Errors validate(AttributeCacheRefreshEvent message) {
            return null;
        }

        @Override
        public void handleBatch(List<AttributeCacheRefreshEvent> messages) {
            String tenant = runtimeTenantResolver.getTenant();

            List<AttributeModel> attributes = attributeHelper.getAllAttributes();
            factory.refresh(tenant, attributes);

            notifClient.notify(String.format(
                                   "Attribute cache refresh finished for microservice %s on project %s. %s attributes detected",
                                   microserviceName,
                                   tenant,
                                   attributes.size()),
                               String.format("[%s] Attribute cache refresh done", microserviceName),
                               NotificationLevel.INFO,
                               DefaultRole.ADMIN);
        }

        @Override
        public boolean isDedicatedDLQEnabled() {
            return false;
        }
    }

}
