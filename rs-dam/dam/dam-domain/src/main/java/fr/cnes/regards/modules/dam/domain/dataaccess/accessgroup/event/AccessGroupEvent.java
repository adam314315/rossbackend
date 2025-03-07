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
package fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;

/**
 * {@link AccessGroup} event common information
 *
 * @author Xavier-Alexandre Brochard
 */
@Event(target = Target.ALL)
public class AccessGroupEvent implements ISubscribable {

    /**
     * The action of the event
     */
    private AccessGroupAction action;

    /**
     * The source of the event
     */
    private AccessGroup accessGroup;

    public AccessGroupEvent() {
        // Deserialization constructor
    }

    public AccessGroupEvent(AccessGroup accessGroup, AccessGroupAction action) {
        this.accessGroup = accessGroup;
        this.action = action;
    }

    public AccessGroupAction getAction() {
        return action;
    }

    public void setAction(AccessGroupAction action) {
        this.action = action;
    }

    public AccessGroup getAccessGroup() {
        return accessGroup;
    }

    public void setAccessGroup(AccessGroup accessGroup) {
        this.accessGroup = accessGroup;
    }

}
