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
package fr.cnes.regards.modules.access.services.client.cache;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * Cache manager for ServiceAggregator
 *
 * @author Sébastien Binda
 */
public class ServiceAggregatorKeyGenerator implements IServiceAggregatorKeyGenerator, InitializingBean {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceAggregatorKeyGenerator.class);

    public static final String KEY_GENERATOR = "ServiceAggregatorKeyGenerator";

    public static final String CACHE_NAME = "serviceAggregator";

    private final IRuntimeTenantResolver tenantResolver;

    private final IAuthenticationResolver authResolver;

    public ServiceAggregatorKeyGenerator(IAuthenticationResolver authResolver, IRuntimeTenantResolver tenantResolver) {
        super();
        this.tenantResolver = tenantResolver;
        this.authResolver = authResolver;
    }

    @Override
    public void afterPropertiesSet() {
        LOGGER.info("___- SERVICE AGGREGATOR CLIENT CACHE -___ Cache is enabled for service aggregator !");
    }

    /**
     * Generates a cache key by adding current tenant, current user authenticated role and method parameters.
     */
    @Override
    public Object generate(Object target, Method method, Object... params) {
        String key = KEY_GENERATOR
                     + "_"
                     + method.getName()
                     + "_"
                     + tenantResolver.getTenant()
                     + "_"
                     + authResolver.getRole()
                     + "_"
                     + StringUtils.arrayToDelimitedString(params, "_");
        LOGGER.debug("Generated key {} for cache {} ", key, CACHE_NAME);
        return key;
    }

    /**
     * Clear cache
     */
    @Override
    @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
    public void cleanCache() {
        // FIXME clean only keys for current tenant
        LOGGER.debug("Cleaning {} cache", CACHE_NAME);
    }

}
