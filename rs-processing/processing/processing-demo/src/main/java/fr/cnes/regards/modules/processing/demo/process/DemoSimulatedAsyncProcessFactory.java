/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.demo.process;

import fr.cnes.regards.framework.amqp.IPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is the demo of asyn process factory.
 *
 * @author gandrieu
 */
@Component
public class DemoSimulatedAsyncProcessFactory {

    private final IPublisher publisher;

    @Autowired
    public DemoSimulatedAsyncProcessFactory(IPublisher publisher) {
        this.publisher = publisher;
    }

    public DemoSimulatedAsyncProcess make() {
        return new DemoSimulatedAsyncProcess(publisher);
    }
}
