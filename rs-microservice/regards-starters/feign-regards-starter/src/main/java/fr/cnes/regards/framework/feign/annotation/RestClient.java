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
package fr.cnes.regards.framework.feign.annotation;

import feign.Headers;
import fr.cnes.regards.framework.feign.FeignClientConfiguration;
import fr.cnes.regards.framework.feign.security.FeignSecurityConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class RestClient
 * <p>
 * Annotation for all microservice clients
 *
 * @author Sébastien Binda
 */
@FeignClient(configuration = { FeignClientConfiguration.class, FeignSecurityConfiguration.class })
@Headers({ "Accept: application/json", "Content-Type: application/json" })
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestClient {

    /**
     * Name of the microservice as it is registered in the eureka server.
     *
     * @return name
     */
    @AliasFor(annotation = FeignClient.class) String name();

    @AliasFor(annotation = FeignClient.class) String contextId() default "";

    /**
     * Fallback class implementation for the client
     *
     * @return Class
     */
    @AliasFor(annotation = FeignClient.class) Class<?> fallback() default void.class;

    /**
     * An absolute URL or resolvable hostname (the protocol is optional).
     */
    @AliasFor(annotation = FeignClient.class) String url() default "";
}
