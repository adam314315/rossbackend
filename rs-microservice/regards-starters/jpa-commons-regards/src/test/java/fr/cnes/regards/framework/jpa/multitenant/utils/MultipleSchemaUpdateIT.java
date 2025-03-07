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

import fr.cnes.regards.framework.jpa.autoconfigure.RegardsFlywayAutoConfiguration;
import fr.cnes.regards.framework.jpa.utils.*;
import fr.cnes.regards.framework.modules.person.Person;
import jakarta.persistence.Entity;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Test updating multiple schema. Just run migration tools
 *
 * @author Marc Sordi
 */
@RunWith(SpringRunner.class)
@TestPropertySource("/multipleSchema.properties")
@ActiveProfiles("test")
@ContextConfiguration(classes = { RegardsFlywayAutoConfiguration.class })
public class MultipleSchemaUpdateIT {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleSchemaUpdateIT.class);

    @Value("${multiple.schema.test.url}")
    private String url;

    @Value("${multiple.schema.test.userName}")
    private String userName;

    @Value("${multiple.schema.test.password}")
    private String password;

    @Value("${multiple.schema.test.driverClassName}")
    private String driver;

    /**
     * Test datasource
     */
    private DataSource dataSource;

    /**
     * Hibernate properties that may impact migration configuration
     */
    private Map<String, Object> hibernateProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setup() throws PropertyVetoException, IOException {
        dataSource = DataSourceHelper.createHikariDataSource("testperson",
                                                             url,
                                                             driver,
                                                             userName,
                                                             password,
                                                             5,
                                                             20,
                                                             "SELECT 1",
                                                             "public",
                                                             5000);

        // Set hibernate properties
        hibernateProperties = new HashMap<>();
        //        String implicitNamingStrategyName = "org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl";
        //        String physicalNamingStrategyName = "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl";
    }

    @After
    public void clean() {
        if (dataSource != null) {
            try {
                dataSource.getConnection().close();
            } catch (SQLException e1) {
                LOGGER.error("Error during closing connection", e1);
            }
        }
    }

    /**
     * hbm2ddl sequence generation doesn't work with multiple schemas in PostgreSQL. Only a single sequence is created for all catalog.<br/>
     * hbm2ddl should not be used in production.
     */
    @Test
    public void testWithHbm2ddl() {

        Hbm2ddlDatasourceSchemaHelper schemaHelper = new Hbm2ddlDatasourceSchemaHelper(hibernateProperties,
                                                                                       Entity.class,
                                                                                       null);

        schemaHelper.migrate(dataSource, Person.class.getPackage().getName(), "hbm2ddl1");
        schemaHelper.migrate(dataSource, Person.class.getPackage().getName(), "hbm2ddl2");
    }

    @Test
    public void testWithFlyway() {

        FlywayDatasourceSchemaHelper migrationHelper = new FlywayDatasourceSchemaHelper(hibernateProperties,
                                                                                        applicationContext);

        String moduleName = "module0";
        ReflectionTestUtils.invokeMethod(migrationHelper, "migrateModule", dataSource, "flyway1", moduleName);
        ReflectionTestUtils.invokeMethod(migrationHelper, "migrateModule", dataSource, "flyway2", moduleName);
        ReflectionTestUtils.invokeMethod(migrationHelper, "migrateModule", dataSource, "flyway3", moduleName);

        moduleName = "module1";
        ReflectionTestUtils.invokeMethod(migrationHelper, "migrateModule", dataSource, "flyway1", moduleName);
        ReflectionTestUtils.invokeMethod(migrationHelper, "migrateModule", dataSource, "flyway2", moduleName);
        ReflectionTestUtils.invokeMethod(migrationHelper, "migrateModule", dataSource, "flyway3", moduleName);
    }

    @Test
    public void testScanModuleScripts() {
        FlywayDatasourceSchemaHelper migrationHelper = new FlywayDatasourceSchemaHelper(hibernateProperties,
                                                                                        applicationContext);
        migrationHelper.migrateSchema(dataSource, "scan");
    }

    @Test
    public void moduleOrderingTest() {
        // plugins < models < entities < dataAccess

        DatabaseModule plugins = new DatabaseModule("plugins");
        DatabaseModule models = new DatabaseModule("models", plugins);
        DatabaseModule entities = new DatabaseModule("entities", plugins, models);
        DatabaseModule dataAccess = new DatabaseModule("dataAccess", plugins, entities);

        List<DatabaseModule> modules = new ArrayList<>();
        modules.add(models);
        modules.add(entities);
        modules.add(dataAccess);
        modules.add(plugins);

        for (DatabaseModule module : modules) {
            module.computeWeight();
        }

        Assert.assertEquals(0, plugins.getWeight());
        Assert.assertEquals(1, models.getWeight());
        Assert.assertEquals(2, entities.getWeight());
        Assert.assertEquals(3, dataAccess.getWeight());

        Collections.sort(modules, new DatabaseModuleComparator());

        Assert.assertEquals(plugins, modules.get(0));
        Assert.assertEquals(models, modules.get(1));
        Assert.assertEquals(entities, modules.get(2));
        Assert.assertEquals(dataAccess, modules.get(3));
    }
}
