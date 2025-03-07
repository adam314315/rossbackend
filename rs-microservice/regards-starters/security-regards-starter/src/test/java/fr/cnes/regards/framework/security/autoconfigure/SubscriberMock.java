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
package fr.cnes.regards.framework.security.autoconfigure;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.ISubscribable;

import java.util.Optional;

/**
 * Class SubscriberMock
 * <p>
 * Test class to mock AMQP Subscriber
 *
 * @author Sébastien Binda
 */
public class SubscriberMock implements ISubscriber {

    @Override
    public <T extends ISubscribable> void subscribeTo(Class<T> pEvent, IHandler<T> pReceiver) {
        // Nothing to do
    }

    @Override
    public <E extends ISubscribable> void subscribeTo(Class<E> eventType,
                                                      IHandler<E> receiver,
                                                      String queueName,
                                                      String exchangeName) {
    }

    @Override
    public <T extends ISubscribable> void unsubscribeFrom(Class<T> pEvent, boolean fast) {

    }

    @Override
    public <E extends ISubscribable> void subscribeTo(Class<E> eventType,
                                                      IHandler<E> receiver,
                                                      String queueName,
                                                      String exchangeName,
                                                      boolean purgeQueue) {
    }

    @Override
    public <E extends ISubscribable> void purgeQueue(Class<E> eventType,
                                                     Class<? extends IHandler<E>> handlerType,
                                                     Optional<String> queueName) {
        // Nothing to do
    }

    @Override
    public void unsubscribeFromAll(boolean fast) {
        // Nothing to do
    }

    @Override
    public void purgeAllQueues(String tenant) {
        // Nothing to do
    }

    @Override
    public void addTenant(String pTenant) {
        // Nothing to do
    }

    @Override
    public void removeTenant(String pTenant) {
        // Nothing to do

    }

    @Override
    public <E extends ISubscribable> void subscribeTo(Class<E> eventType, IHandler<E> receiver, boolean purgeQueue) {
        /// Nothing to do
    }
}
