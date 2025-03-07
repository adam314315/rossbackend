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
package fr.cnes.regards.framework.amqp.configuration;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationDtoBuilder;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ErrorHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Marc SORDI
 */
public class RegardsErrorHandler implements ErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegardsErrorHandler.class);

    private static final String AMQP_DLQ_FAILURE_MESSAGE = "Message failure";

    private final IInstancePublisher instancePublisher;

    private final IPublisher publisher;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final String microserviceName;

    @Value("${regards.instance.tenant.name:instance}")
    private String instanceTenantName;

    public RegardsErrorHandler(IRuntimeTenantResolver runtimeTenantResolver,
                               IInstancePublisher instancePublisher,
                               IPublisher publisher,
                               String microserviceName) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.instancePublisher = instancePublisher;
        this.publisher = publisher;
        this.microserviceName = microserviceName;
    }

    /**
     * This method notifies AMQP failure to INSTANCE_ADMIN and if possible to PROJECT_ADMIN.<br/>
     * To notify PROJECT_ADMIN, this method uses only work with tenant header.
     */
    @Override
    public void handleError(Throwable t) {

        // Try to notify message
        if (ListenerExecutionFailedException.class.isAssignableFrom(t.getClass())) {
            ListenerExecutionFailedException lefe = (ListenerExecutionFailedException) t;
            LOGGER.error("AMQP failed message : {}", lefe.getFailedMessage());

            // Build event
            // Message#toString is already handling encoding and content type if possible
            String message = "AMQP message has been routed to DLQ (dead letter queue).";
            Set<String> roles = new HashSet<>(Collections.singletonList(DefaultRole.PROJECT_ADMIN.toString()));
            NotificationEvent event = NotificationEvent.build(new NotificationDtoBuilder(message,
                                                                                         AMQP_DLQ_FAILURE_MESSAGE,
                                                                                         NotificationLevel.ERROR,
                                                                                         microserviceName).toRoles(roles));

            // Publish to INSTANCE ADMIN
            instancePublisher.publish(event);

            // Try to publish to PROJECT_ADMIN looking for tenant to properly route the notification
            String tenant = lefe.getFailedMessage()
                                .getMessageProperties()
                                .getHeader(AmqpConstants.REGARDS_TENANT_HEADER);
            if (StringUtils.isNotBlank(tenant) && !instanceTenantName.equals(tenant)) {
                try {
                    // Route notification to the right tenant
                    runtimeTenantResolver.forceTenant(tenant);
                    publisher.publish(event);
                } finally {
                    runtimeTenantResolver.clearTenant();
                }
            } else {
                LOGGER.error("Invalid message. Missing tenant header !");
            }
        } else {
            NotificationDtoBuilder notifBuilder = new NotificationDtoBuilder(t.getMessage(),
                                                                             AMQP_DLQ_FAILURE_MESSAGE,
                                                                             NotificationLevel.ERROR,
                                                                             microserviceName);
            Set<String> roles = new HashSet<>();
            roles.add(DefaultRole.PROJECT_ADMIN.name());
            // Publish to INSTANCE ADMIN
            instancePublisher.publish(NotificationEvent.build(notifBuilder.toRoles(roles)));
        }

        LOGGER.error("Listener failed - message rerouted to dead letter queue", t);
    }

}
