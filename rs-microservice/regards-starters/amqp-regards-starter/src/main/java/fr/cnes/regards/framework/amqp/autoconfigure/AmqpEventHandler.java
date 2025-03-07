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
package fr.cnes.regards.framework.amqp.autoconfigure;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.VirtualHostMode;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.tenant.TenantCreatedEvent;
import fr.cnes.regards.framework.amqp.event.tenant.TenantDeletedEvent;
import org.springframework.beans.factory.InitializingBean;

/**
 * This class helps to configure virtual hosts at runtime listening to tenant events. The system uses the AMQP manager
 * virtual host no to be tenant dependent.
 *
 * @author Marc Sordi
 */
public class AmqpEventHandler implements InitializingBean {

    /**
     * Used to configure tenant virtual hosts
     */
    private final IRabbitVirtualHostAdmin virtualHostAdmin;

    /**
     * Used to listen to tenant events
     */
    private final IInstanceSubscriber instanceSubscriber;

    /**
     * Used to update tenant listeners
     */
    private final ISubscriber subscriber;

    public AmqpEventHandler(IRabbitVirtualHostAdmin pVirtualHostAdmin,
                            IInstanceSubscriber pInstanceSubscriber,
                            ISubscriber pSubscriber) {
        this.virtualHostAdmin = pVirtualHostAdmin;
        this.instanceSubscriber = pInstanceSubscriber;
        this.subscriber = pSubscriber;
    }

    /**
     * Manage virtual hosts according to tenants
     */
    @Override
    public void afterPropertiesSet() {
        // Listen to tenant events
        instanceSubscriber.subscribeTo(TenantCreatedEvent.class, new TenantCreationHandler());
        instanceSubscriber.subscribeTo(TenantDeletedEvent.class, new TenantDeletionHandler());
    }

    /**
     * Handle tenant creation
     *
     * @author Marc Sordi
     */
    private class TenantCreationHandler implements IHandler<TenantCreatedEvent> {

        @Override
        public void handle(String tenant, TenantCreatedEvent tce) {
            if (VirtualHostMode.MULTI.equals(virtualHostAdmin.getMode())) {
                virtualHostAdmin.addVhost(RabbitVirtualHostAdmin.getVhostName(tce.getTenant()));
            }
            subscriber.addTenant(tce.getTenant());
        }
    }

    /**
     * Handle tenant deletion
     *
     * @author Marc Sordi
     */
    private class TenantDeletionHandler implements IHandler<TenantDeletedEvent> {

        @Override
        public void handle(String tenant, TenantDeletedEvent tde) {
            subscriber.removeTenant(tde.getTenant());
            if (VirtualHostMode.MULTI.equals(virtualHostAdmin.getMode())) {
                virtualHostAdmin.removeVhost(tde.getTenant());
            }
        }
    }
}
