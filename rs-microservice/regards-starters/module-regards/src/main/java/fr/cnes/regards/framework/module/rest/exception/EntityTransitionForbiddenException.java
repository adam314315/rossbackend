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
package fr.cnes.regards.framework.module.rest.exception;

/**
 * Exception thrown when an illegal transition is called on an entity which state is managed by a workflow
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */

public class EntityTransitionForbiddenException extends EntityOperationForbiddenException {

    public <T> EntityTransitionForbiddenException(final String entityIdentifier,
                                                  final Class<?> entityClass,
                                                  final String state,
                                                  final String transition) {
        super(entityIdentifier,
              entityClass,
              "The transition "
              + transition
              + " called on this state-managed entity is illegal for its current state "
              + state);
    }

}