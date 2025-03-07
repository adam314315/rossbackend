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
package fr.cnes.regards.framework.multitenant.test;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Single tenant resolver. Useful for testing purpose. Add multi-thread management.
 *
 * @author Marc Sordi
 * @author oroussel
 */
public class SingleRuntimeTenantResolver implements IRuntimeTenantResolver {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleRuntimeTenantResolver.class);

    protected static final String TENANT = "tenant";

    // Thread safe tenant holder for forced tenant
    private static final ThreadLocal<String> forcedTenantHolder = new ThreadLocal<>();

    private static final ThreadLocal<String> currentTenantHolder = new ThreadLocal<>();

    public SingleRuntimeTenantResolver(String tenant) {
        currentTenantHolder.set(tenant);
    }

    @Override
    public String getTenant() {
        // Try to get tenant from tenant holder
        String tenant = forcedTenantHolder.get();
        if (tenant != null) {
            return tenant;
        }
        // Try to get current tenant
        return currentTenantHolder.get();
    }

    @Override
    public void forceTenant(String tenant) {
        MDC.put(TENANT, tenant);
        forcedTenantHolder.set(tenant);
    }

    @Override
    public boolean isInstance() {
        return Boolean.FALSE;
    }

    @Override
    public void clearTenant() {
        forcedTenantHolder.remove();
    }

}
