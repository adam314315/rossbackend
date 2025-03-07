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
package fr.cnes.regards.framework.jpa.instance.properties;

import fr.cnes.regards.framework.jpa.utils.MigrationTool;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Class InstanceDaoProperties
 * <p>
 * DAO Instance database configuration
 *
 * @author Sébastien Binda
 */
@ConfigurationProperties("regards.jpa.instance")
public class InstanceDaoProperties {

    /**
     * Instance transactions manager identifier
     */
    public static final String INSTANCE_TRANSACTION_MANAGER = "instanceJpaTransactionManager";

    /**
     * Is Instance database enabled ?
     */
    private Boolean enabled;

    /**
     * Instance JPA Datasource
     */
    @NestedConfigurationProperty
    private DataSourceProperties datasource;

    /**
     * Does the instance dao is embedded ?
     */
    private Boolean embedded = Boolean.FALSE;

    /**
     * Path for embedded databases
     */
    private String embeddedPath;

    /**
     * Hibernate dialect
     */
    private String dialect = "org.hibernate.dialect.PostgreSQLDialect";

    /**
     * For pooled data source, min available connections
     */
    private Integer minPoolSize = 1;

    /**
     * For pooled data source, max available connections
     */
    private Integer maxPoolSize = 5;

    /**
     * Threshold of time in ms of connection acquisition to log warn with the connection acquisition duration time.
     */
    private Long connectionAcquisitionThresholdLoggerLimit = 5_000L;

    /**
     * Default test query
     */
    private String preferredTestQuery = "SELECT 1";

    /**
     * Migration tool. Default to hbm2ddl.
     */
    private MigrationTool migrationTool = MigrationTool.FLYWAYDB;

    /**
     * Optional script output file for {@link MigrationTool#HBM2DDL}
     */
    private String outputFile = null;

    /**
     * Setter
     *
     * @param pDatasource instance JPA datasource
     */
    public void setDatasource(final DataSourceProperties pDatasource) {
        this.datasource = pDatasource;
    }

    /**
     * Getter
     *
     * @return instance JPA Datasource
     */
    public DataSourceProperties getDatasource() {
        return this.datasource;
    }

    /**
     * Getter
     *
     * @return Is Instance database enabled ?
     * @since 1.0 SNAPSHOT
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Setter
     *
     * @param pEnabled Is Instance database enabled ?
     * @since 1.0 SNAPSHOT
     */
    public void setEnabled(final Boolean pEnabled) {
        enabled = pEnabled;
    }

    public Boolean getEmbedded() {
        return embedded;
    }

    public void setEmbedded(final Boolean pEmbedded) {
        embedded = pEmbedded;
    }

    public String getEmbeddedPath() {
        return embeddedPath;
    }

    public void setEmbeddedPath(final String pEmbeddedPath) {
        embeddedPath = pEmbeddedPath;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(final String pDialect) {
        dialect = pDialect;
    }

    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(Integer pMinPoolSize) {
        minPoolSize = pMinPoolSize;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer pMaxPoolSize) {
        maxPoolSize = pMaxPoolSize;
    }

    public String getPreferredTestQuery() {
        return preferredTestQuery;
    }

    public void setPreferredTestQuery(String pPreferredTestQuery) {
        preferredTestQuery = pPreferredTestQuery;
    }

    public MigrationTool getMigrationTool() {
        return migrationTool;
    }

    public void setMigrationTool(MigrationTool pMigrationTool) {
        migrationTool = pMigrationTool;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String pOutputFile) {
        outputFile = pOutputFile;
    }

    public Long getConnectionAcquisitionThresholdLoggerLimit() {
        return connectionAcquisitionThresholdLoggerLimit;
    }

    public void setConnectionAcquisitionThresholdLoggerLimit(Long connectionAcquisitionThresholdLoggerLimit) {
        this.connectionAcquisitionThresholdLoggerLimit = connectionAcquisitionThresholdLoggerLimit;
    }
}
