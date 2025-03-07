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
package fr.cnes.regards.modules.crawler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Asynchronous task configuration
 *
 * @author Marc Sordi
 */
@Configuration
@ConditionalOnMissingBean(AsyncConfigurer.class)
@EnableAsync
@Profile("!nocrawl")
public class CrawlerServiceConfiguration implements AsyncConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerService.class);

    private static final String HR = "*****************************************************";

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            LOGGER.error(HR);
            LOGGER.error("Exception message - " + throwable.getMessage(), throwable);
            LOGGER.error("Method name - " + method.getName());
            for (Object param : params) {
                LOGGER.error("Parameter value - " + param);
            }
            LOGGER.error(HR);
        };
    }
}
