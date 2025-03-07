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

package fr.cnes.regards.modules.dam.domain.datasources.plugins;

public final class DBConnectionPluginConstants {

    private DBConnectionPluginConstants() {
    }

    /**
     * User name
     */
    public static final String USER_PARAM = "user";

    /**
     * User password
     */
    public static final String PASSWORD_PARAM = "password"; // NOSONAR

    /**
     * Database host
     */
    public static final String DB_HOST_PARAM = "dbHost";

    /**
     * Database port
     */
    public static final String DB_PORT_PARAM = "dbPort";

    /**
     * Database name
     */
    public static final String DB_NAME_PARAM = "dbName";

    /**
     * Database driver
     */
    public static final String DRIVER_PARAM = "driver";
}
