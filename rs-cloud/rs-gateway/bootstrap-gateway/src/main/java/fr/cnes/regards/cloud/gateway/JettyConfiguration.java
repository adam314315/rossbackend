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
package fr.cnes.regards.cloud.gateway;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This configuration is needed to manage timeout
 *
 * @author oroussel
 */
@Configuration
public class JettyConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyConfiguration.class);

    @Value("${jetty.threadPool.idleTimeout:3600000}")
    private int threadPoolIdleTimeout;

    @Value("${jetty.threadPool.maxThreads:200}")
    private int threadPoolMaxThreads;

    @Bean
    public WebServerFactoryCustomizer<JettyServletWebServerFactory> customizer() {
        LOGGER.info("Customizing Jetty server...");
        return jetty -> {
            QueuedThreadPool threadPool = new QueuedThreadPool();
            LOGGER.info("Setting Jetty server thread pool idle timeout to {} ms", threadPoolIdleTimeout);
            threadPool.setIdleTimeout(threadPoolIdleTimeout);
            threadPool.setMaxThreads(threadPoolMaxThreads);
            jetty.setThreadPool(threadPool);
            jetty.addServerCustomizers(server -> {
                for (Connector connector : server.getConnectors()) {
                    if (connector instanceof ServerConnector) {
                        ((ServerConnector) connector).setIdleTimeout(threadPoolIdleTimeout);
                    }
                }
            });
        };
    }
}
