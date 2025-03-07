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
package fr.cnes.regards.framework.feign.security;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class allows to customize Feign behavior.<br>
 * This class has to be annotated with <code>@Configuration</code>. <br/>
 * It uses an internal JWT with a system role to call another microservice.
 *
 * @author Marc Sordi
 */
@Configuration
public class FeignSecurityConfiguration {

    /**
     * Interceptor for Feign client request security. This interceptor injects a token into request headers.
     *
     * @param feignSecurityManager the Feign security manager
     * @return RequestInterceptor custom system interceptor
     */
    @Bean
    public RequestInterceptor securityRequestInterceptor(FeignSecurityManager feignSecurityManager) {
        return new FeignSecurityInterceptor(feignSecurityManager);
    }
}
