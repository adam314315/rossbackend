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
package fr.cnes.regards.framework.amqp.test.handler;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event counter
 *
 * @author Marc Sordi
 */
public abstract class AbstractNoWrapperReceiver<T> implements IHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNoWrapperReceiver.class);

    private int count = 0;

    @Override
    public void handle(String tenant, T message) {
        doHandle(tenant, message);
        count++;
        LOGGER.debug("Info count {}", count);
        LOGGER.debug("Tenant : {}", tenant);
        LOGGER.debug("Content type : {}", message.getClass());
        LOGGER.debug("Handler type : {}", this.getClass());
    }

    /**
     * Override this method to do custom stuff!
     *
     * @param wrapper message wrappe
     */
    protected void doHandle(String tenant, T message) {
        // Nothing to do here
    }

    public Integer getCount() {
        return count;
    }

    public void resetCount() {
        this.count = 0;
    }

    /**
     * @param count count
     * @return true if handler has been reached "count" times
     */
    public boolean checkCount(int count) {
        if (this.count != count) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Assert.fail("Thread interrupted");
            }
        }
        return (this.count == count);
    }

    /**
     * Assert handler has been reached "count" times
     */
    public void assertCount(Integer count) {
        Assert.assertTrue(checkCount(count));
    }
}
