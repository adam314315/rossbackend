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
package fr.cnes.regards.framework.amqp.autoconfigure;

import fr.cnes.regards.framework.amqp.AmqpHealthIndicator;
import fr.cnes.regards.framework.amqp.IPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.amqp.RabbitHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Adapted Rabbit Health indicator
 *
 * @author Marc SORDI
 * <p>
 * FIXME : WIP with spring boot admin
 */
@AutoConfiguration(before = RabbitHealthContributorAutoConfiguration.class)
@ConditionalOnProperty(prefix = "regards.amqp", name = "enabled", matchIfMissing = true)
public class AmqpHealthIndicatorAutoConfiguration {

    @Autowired
    private IPublisher publisher;

    // Override RabbitMQ health indicator
    @Bean(name = "rabbitHealthIndicator")
    @ConditionalOnMissingBean(name = "rabbitHealthIndicator")
    public HealthIndicator rabbitHealthIndicator() {
        return new AmqpHealthIndicator(publisher);
    }
}
