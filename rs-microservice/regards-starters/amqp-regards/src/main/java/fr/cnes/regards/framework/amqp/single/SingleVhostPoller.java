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
package fr.cnes.regards.framework.amqp.single;

import fr.cnes.regards.framework.amqp.AbstractPoller;
import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.amqp.configuration.AmqpChannel;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Single virtual host publisher implementation
 *
 * @author Marc Sordi
 */
public class SingleVhostPoller extends AbstractPoller implements IPoller {

    private final IRuntimeTenantResolver threadTenantResolver;

    public SingleVhostPoller(IRabbitVirtualHostAdmin virtualHostAdmin,
                             RabbitTemplate rabbitTemplate,
                             IAmqpAdmin amqpAdmin,
                             IRuntimeTenantResolver threadTenantResolver) {
        super(virtualHostAdmin, rabbitTemplate, amqpAdmin);
        this.threadTenantResolver = threadTenantResolver;
    }

    @Override
    protected String resolveTenant() {
        return threadTenantResolver.getTenant();
    }

    @Override
    protected String resolveVirtualHost(String tenant) {
        return AmqpChannel.AMQP_MULTITENANT_MANAGER;
    }
}
