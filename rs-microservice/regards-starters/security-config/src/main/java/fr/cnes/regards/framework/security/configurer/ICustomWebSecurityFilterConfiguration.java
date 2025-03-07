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
package fr.cnes.regards.framework.security.configurer;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.Set;

/**
 * Interface to define specific WebSecurity filter configuration
 * <p>
 * To define WebSecurity autorizeRequests, see {@link ICustomWebSecurityAuthorizeRequestsConfiguration}
 *
 * @author Leo Mieulet
 */
@FunctionalInterface
public interface ICustomWebSecurityFilterConfiguration {

    /**
     * Configure HttpSecurity filters
     *
     * @param http             HttpSecurity
     * @param noSecurityRoutes list of routes the filter should avoid to filter
     * @throws CustomWebSecurityConfigurationException configuration exception
     */
    void configure(final HttpSecurity http, Set<String> noSecurityRoutes)
        throws CustomWebSecurityConfigurationException;

}
