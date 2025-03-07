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
package fr.cnes.regards.framework.jpa.multitenant.utils;

import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Help to init tenant data sources
 *
 * @author Marc Sordi
 */
public final class TenantDataSourceHelper {

    private static final String QUERY_PARAM_DELIMITER = "?";

    private static final String QUERY_PARAM_SEPARATOR = "&";

    private static final String QUERY_PARAM_KV_SEPARATOR = "=";

    private TenantDataSourceHelper() {
    }

    /**
     * Init a tenant data source
     *
     * @param daoProperties    {@link MultitenantDaoProperties}
     * @param tenantConnection tenant connection to create
     * @return a {@link DataSource}
     * @throws PropertyVetoException if parameter not supported
     * @throws SQLException          if connection fails
     */
    public static DataSource initDataSource(MultitenantDaoProperties daoProperties,
                                            TenantConnection tenantConnection,
                                            String schemaIdentifier)
        throws PropertyVetoException, SQLException, IOException {
        DataSource dataSource;
        // Bypass configuration if embedded enabled
        if (daoProperties.getEmbedded()) {
            // Create an embedded data source
            dataSource = DataSourceHelper.createEmbeddedDataSource(tenantConnection.getTenant(),
                                                                   daoProperties.getEmbeddedPath());
        } else {
            addApplicationName(tenantConnection, schemaIdentifier);
            // Create a pooled data source
            dataSource = DataSourceHelper.createHikariDataSource(tenantConnection.getTenant(),
                                                                 tenantConnection.getUrl(),
                                                                 tenantConnection.getDriverClassName(),
                                                                 tenantConnection.getUserName(),
                                                                 tenantConnection.getPassword(),
                                                                 daoProperties.getMinPoolSize(),
                                                                 daoProperties.getMaxPoolSize(),
                                                                 daoProperties.getPreferredTestQuery(),
                                                                 schemaIdentifier,
                                                                 daoProperties.getConnectionAcquisitionThresholdLoggerLimit());

            // Test connection for pooled datasource
            DataSourceHelper.testConnection(dataSource, true);
        }
        return dataSource;
    }

    private static void addApplicationName(TenantConnection connection, String appName) {
        String appKey = "ApplicationName";
        if (!connection.getUrl().contains(appKey)) {
            StringBuilder buffer = new StringBuilder(connection.getUrl());
            if (!connection.getUrl().contains(QUERY_PARAM_DELIMITER)) {
                buffer.append(QUERY_PARAM_DELIMITER);
            } else {
                buffer.append(QUERY_PARAM_SEPARATOR);
            }
            buffer.append(appKey);
            buffer.append(QUERY_PARAM_KV_SEPARATOR);
            buffer.append(appName);
            connection.setUrl(buffer.toString());
        }
    }

    public static void verifyBatchParameter(JpaProperties properties, TenantConnection connection) {
        String rbi = "reWriteBatchedInserts";
        String batchSize = properties.getProperties().get("hibernate.jdbc.batch_size");
        if ((batchSize != null) && TenantConnection.DEFAULT_DRIVER_CLASS_NAME.equals(connection.getDriverClassName())) {
            if (connection.getUrl().contains(rbi)) {
                // Nothing to do
            } else {
                StringBuilder buffer = new StringBuilder(connection.getUrl());
                if (!connection.getUrl().contains(QUERY_PARAM_DELIMITER)) {
                    buffer.append(QUERY_PARAM_DELIMITER);
                } else {
                    buffer.append(QUERY_PARAM_SEPARATOR);
                }
                buffer.append(rbi);
                buffer.append(QUERY_PARAM_KV_SEPARATOR);
                buffer.append("true");
                connection.setUrl(buffer.toString());
            }
        }
    }
}
