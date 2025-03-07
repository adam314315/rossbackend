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

import fr.cnes.regards.framework.amqp.configuration.AmqpChannel;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

/**
 * Common poller methods
 *
 * @author svissier
 * @author Marc Sordi
 */
public abstract class AbstractPoller implements IPollerContract {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPoller.class);

    /**
     * bean provided by spring to receive message from broker
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * bean assisting us to declare elements
     */
    private final IAmqpAdmin amqpAdmin;

    /**
     * Virtual host admin
     */
    private final IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    public AbstractPoller(IRabbitVirtualHostAdmin pVirtualHostAdmin,
                          RabbitTemplate rabbitTemplate,
                          IAmqpAdmin amqpAdmin) {
        super();
        this.rabbitVirtualHostAdmin = pVirtualHostAdmin;
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
    }

    @Override
    public <T extends IPollable> T poll(Class<T> pEvent) {
        String tenant = resolveTenant();
        return poll(tenant,
                    resolveVirtualHost(tenant),
                    pEvent,
                    WorkerMode.UNICAST,
                    EventUtils.getTargetRestriction(pEvent));
    }

    /**
     * @return current tenant
     */
    protected abstract String resolveTenant();

    /**
     * @param tenant current tenant
     * @return the virtual host on which we have to poll the event according to the tenant
     */
    protected abstract String resolveVirtualHost(String tenant);

    /**
     * Poll an event
     *
     * @param <T>         event object
     * @param tenant      tenant
     * @param virtualHost virtual host
     * @param eventType   event to poll
     * @param workerMode  {@link WorkerMode}
     * @param target      {@link Target}
     * @return event
     */
    @SuppressWarnings("unchecked")
    protected <T> T poll(String tenant, String virtualHost, Class<T> eventType, WorkerMode workerMode, Target target) {

        LOGGER.debug("Polling event {} for tenant {} (Target : {}, WorkerMode : {} )",
                     eventType.getName(),
                     tenant,
                     target,
                     workerMode);

        try {
            // Bind the connection to the right vHost (i.e. tenant to publish the message)
            rabbitVirtualHostAdmin.bind(virtualHost);

            AmqpChannel channel = AmqpChannel.build(eventType, workerMode, target);
            Exchange exchange = amqpAdmin.declareExchange(channel);
            Queue queue = amqpAdmin.declareQueue(tenant, channel);
            amqpAdmin.declareBinding(queue, exchange, workerMode, Optional.empty());

            // routing key is unnecessary for fanout exchanges but is for direct exchanges
            return (T) rabbitTemplate.receiveAndConvert(queue.getName(), 0);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }
}
