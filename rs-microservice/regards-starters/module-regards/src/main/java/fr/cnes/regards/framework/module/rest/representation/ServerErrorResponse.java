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
package fr.cnes.regards.framework.module.rest.representation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Server error response representation
 *
 * @author Marc Sordi
 */
public class ServerErrorResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerErrorResponse.class);

    /**
     * Error message
     */
    private final List<String> messages;

    public ServerErrorResponse(String message, Throwable throwable) {
        this(Collections.singletonList(message), throwable);
    }

    public ServerErrorResponse(List<String> messages, Throwable throwable) {
        this.messages = messages;
        if (messages != null) {
            messages.forEach(LOGGER::error);
        }
        LOGGER.debug(throwable.getMessage(), throwable);
    }

    public List<String> getMessages() {
        return messages;
    }
}
