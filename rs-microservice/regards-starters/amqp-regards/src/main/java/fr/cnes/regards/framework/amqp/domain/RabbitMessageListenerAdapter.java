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
package fr.cnes.regards.framework.amqp.domain;

import com.rabbitmq.client.Channel;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.MessageConversionException;

/**
 * @author Marc SORDI
 */
public class RabbitMessageListenerAdapter extends MessageListenerAdapter {

    public RabbitMessageListenerAdapter(Object delegate, String defaultListenerMethod) {
        super(delegate, defaultListenerMethod);
    }

    @Override
    protected Object[] buildListenerArguments(Object extractedMessage, Channel channel, Message message) {
        String tenant = message.getMessageProperties().getHeader(AmqpConstants.REGARDS_TENANT_HEADER);
        if (tenant != null && !tenant.isEmpty()) {
            return new Object[] { tenant, extractedMessage };
        } else {
            throw new MessageConversionException("Invalid message. Missing tenant header.");
        }
    }
}
