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
package fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional;

import fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional.pojo.Company;
import fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional.pojo.User;
import fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional.repository.ICompanyRepository;
import fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional.repository.IUserRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.ArrayList;
import java.util.List;

/**
 * Class MultiTenancyDaoIT
 * <p>
 * Unit tests for multitenancy DAO
 *
 * @author CS
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { MultiTenancyDaoTestConfiguration.class })
@ActiveProfiles("test")
public class MultiTenancyDaoIT {

    /**
     * class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MultiTenancyDaoIT.class);

    /**
     * Tenant name for test1
     */
    private static final String TENANT_TEST_1 = "test1";

    /**
     * Tenant name for test2
     */
    private static final String TENANT_TEST_2 = "test2";

    /**
     * Tenant name for invalid tenant (does not exists)
     */
    private static final String TENANT_INVALID = "invalid";

    /**
     * JPA User repository
     */
    @Autowired
    private IUserRepository userRepository;

    /**
     * JPA Company repository
     */
    @Autowired
    private ICompanyRepository companyRepository;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Unit test to check that the spring JPA multitenancy context is loaded successfully
     * <p>
     * S
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check that the spring JPA multitenancy context is loaded successfully")
    @Test
    public void contextLoads() {
        // Nothing to do. Only tests if the spring context is ok.
    }

    /**
     * Unit test to check JPA foreign keys management
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check JPA foreign keys management")
    @Test
    public void foreignKeyTests() {

        runtimeTenantResolver.forceTenant(TENANT_TEST_1);
        userRepository.deleteAll();
        final Company comp = companyRepository.save(new Company("plop"));
        userRepository.save(new User("name", "lastname", comp));
        Assert.assertTrue(userRepository.findAll().iterator().next().getCompany().getId().equals(comp.getId()));
    }

    /**
     * Unit test to check JPA uses the good tenant through the tenant resolver
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check that JPA uses the good tenant through the tenant resolver")
    @Test
    public void multitenancyAccessTest() {
        final List<User> results = new ArrayList<>();

        // Set tenant to project test1
        runtimeTenantResolver.forceTenant(TENANT_TEST_1);
        // Delete all previous data if any
        userRepository.deleteAll();
        // Set tenant to project 2
        runtimeTenantResolver.forceTenant(TENANT_TEST_2);
        // Delete all previous data if any
        userRepository.deleteAll();

        // Set tenant to project test1
        runtimeTenantResolver.forceTenant(TENANT_TEST_1);
        // Add new users
        User newUser = new User("Jean", "Pont");
        newUser = userRepository.save(newUser);
        User newUser2 = new User("Alain", "Deloin");
        newUser2 = userRepository.save(newUser2);

        // Check results
        Iterable<User> list = userRepository.findAll();
        list.forEach(results::add);
        Assert.assertEquals("Error, there must be 2 elements in the database associated to the tenant test1 not "
                            + results.size(), 2, results.size());

        // Set tenant to project 2
        runtimeTenantResolver.forceTenant(TENANT_TEST_2);

        // Check that there is no users added on this project
        list = userRepository.findAll();
        results.clear();
        list.forEach(results::add);
        Assert.assertEquals("Error, there must be no element in the database associated to the tenant test2 ("
                            + results.size()
                            + ")", 0, results.size());

        newUser = userRepository.save(newUser);
        LOG.info("id=" + newUser.getId());

        // Check results
        list = userRepository.findAll();
        results.clear();
        list.forEach(results::add);
        Assert.assertEquals("Error, there must be 1 elements in the database associated to the tenant test2 + not "
                            + results.size(), 1, results.size());

        // Set tenant to an non existing project
        runtimeTenantResolver.forceTenant(TENANT_INVALID);
        try {
            // Check that an exception is thrown
            list = userRepository.findAll();
            Assert.fail("This repository is not valid for tenant");
        } catch (final CannotCreateTransactionException e) {
            // Nothing to do
        }
    }
}
