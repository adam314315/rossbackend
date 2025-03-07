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
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class DefaultTenantConnectionResolver
 * <p>
 * Default resolver. Return empty list
 *
 * @author Sébastien Binda
 * @author Marc Sordi
 */
public class DefaultTenantConnectionResolver implements ITenantConnectionResolver {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTenantConnectionResolver.class);

    @Override
    public List<TenantConnection> getTenantConnections(String microserviceName) {
        LOGGER.warn("No Tenant connections resolver defined. Default one used.");
        return new ArrayList<>();
    }

    @Override
    public void addTenantConnection(String microserviceName, final TenantConnection pTenantConnection) {
        LOGGER.warn("No Tenant connections resolver defined. Tenant connection is not persisted.");
    }

    @Override
    public void updateState(String microservice,
                            String tenant,
                            TenantConnectionState state,
                            Optional<String> errorCause) {
        LOGGER.warn("No Tenant connections resolver defined. Tenant connection is not updated.");
    }

}
