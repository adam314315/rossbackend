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
package fr.cnes.regards.modules.opensearch.rest;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;

/**
 * @author sbinda
 */
@MultitenantTransactional
@TestPropertySource("classpath:test.properties")
@Ignore("TODO: simulate a web server that serves opensearch descriptor instead of unsable peps server")
public class OpensearchControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchControllerIT.class);

    @Test
    @Requirement("REGARDS_DSL_DAM_SET_810")
    @Ignore("Replace peps URL by an http mock")
    public void testRetrieveAccessGroupsListOfUser() {
        performDefaultGet(OpensearchController.TYPE_MAPPING + "/descriptor",
                          customizer().expectStatusOk()
                                      .expectIsNotEmpty(JSON_PATH_ROOT)
                                      .addParameter("url",
                                                    "https://peps.cnes.fr/resto/api/collections/S1/describe.xml"),
                          "error");
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
