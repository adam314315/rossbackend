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
package fr.cnes.regards.modules.order.dao;

import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.SearchRequestParameters;

/**
 * @author Théo Lasserre
 */
public class RequestSpecificationsBuilder extends AbstractSpecificationsBuilder<Order, SearchRequestParameters> {

    @Override
    protected void addSpecificationsFromParameters() {
        if (parameters != null) {
            specifications.add(like("owner", parameters.getOwner()));
            specifications.add(useDatesRestriction("creationDate", parameters.getCreationDate()));
            specifications.add(useValuesRestriction("status", parameters.getStatuses()));
            specifications.add(equals("waitingForUser", parameters.getWaitingForUser()));
        }
    }
}
