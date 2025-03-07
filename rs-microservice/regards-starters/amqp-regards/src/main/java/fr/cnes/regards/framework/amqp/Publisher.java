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
package fr.cnes.regards.framework.amqp;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RabbitVirtualHostAdmin;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

/**
 * {@link Publisher} uses {@link IRuntimeTenantResolver} to resolve current thread tenant to publish an event in the
 * multitenant context.
 *
 * @author svissier
 * @author lmieulet
 * @author Marc Sordi
 */
public class Publisher extends AbstractPublisher implements IPublisher {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(Publisher.class);

    /**
     * Resolve thread tenant
     */
    private final IRuntimeTenantResolver threadTenantResolver;

    public Publisher(String applicationId,
                     IRabbitVirtualHostAdmin pVirtualHostAdmin,
                     RabbitTemplate rabbitTemplate,
                     RabbitAdmin rabbitAdmin,
                     IAmqpAdmin amqpAdmin,
                     IRuntimeTenantResolver pThreadTenantResolver,
                     Gson gson,
                     List<String> eventsToNotifier) {
        super(rabbitTemplate, rabbitAdmin, amqpAdmin, pVirtualHostAdmin, applicationId, gson, eventsToNotifier);
        this.threadTenantResolver = pThreadTenantResolver;
    }

    @Override
    protected String resolveTenant() {
        return threadTenantResolver.getTenant();
    }

    @Override
    protected String resolveVirtualHost(String tenant) {
        return RabbitVirtualHostAdmin.getVhostName(tenant);
    }
}
