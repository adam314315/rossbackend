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
package fr.cnes.regards.framework.amqp.configuration;

/**
 * AMQP utility constants
 *
 * @author Marc Sordi
 */
public final class AmqpConstants {

    /**
     * Headers
     */
    public static final String REGARDS_HEADER_NS = "regards.";

    public static final String REGARDS_TENANT_HEADER = REGARDS_HEADER_NS + "tenant";

    public static final String REGARDS_CONVERTER_HEADER = REGARDS_HEADER_NS + "converter";

    /**
     * Header necessary for Jackson deserialization, in order to harmonize the process, it's used for both Jackson
     * and Gson
     */
    public static final String REGARDS_TYPE_HEADER = "__TypeId__";

    /**
     * Legacy header for Gson deserialization, it's kept to be able to deserialize old messages after an update of
     * regards, but will be removed at some point.
     */
    public static final String REGARDS_TYPE_HEADER_LEGACY = REGARDS_HEADER_NS + "type";

    /**
     * Request headers
     */
    public static final String REGARDS_REQUEST_ID_HEADER = REGARDS_HEADER_NS + "request.id";

    public static final String REGARDS_REQUEST_DATE_HEADER = REGARDS_HEADER_NS + "request.date";

    public static final String REGARDS_REQUEST_OWNER_HEADER = REGARDS_HEADER_NS + "request.owner";

    private AmqpConstants() {
    }
}
