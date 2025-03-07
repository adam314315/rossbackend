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
package fr.cnes.regards.modules.dam.dao.dataaccess;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalIT;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=dam_ag_dao" })
public class AccessGroupRepositoryIT extends AbstractDaoTransactionalIT {

    private static final String AG1_NAME = "AG1";

    private static final String AG2_NAME = "AG2";

    private static final String AG3_NAME = "AG3";

    @Autowired
    private IAccessGroupRepository dao;

    private AccessGroup ag1;

    private AccessGroup ag2;

    private AccessGroup ag3;

    @Before
    public void init() {
        ag1 = new AccessGroup(AG1_NAME);
        ag1 = dao.save(ag1);
        ag2 = new AccessGroup(AG2_NAME);
        ag2 = dao.save(ag2);
        ag3 = new AccessGroup(AG3_NAME);
        ag3.setPublic(Boolean.TRUE);
        ag3 = dao.save(ag3);
    }

    @Test
    public void testFindOneByName() {
        AccessGroup agOfNameAG1 = dao.findOneByName(AG1_NAME);
        Assert.assertEquals(ag1, agOfNameAG1);
        AccessGroup agOfNameAG2 = dao.findOneByName(AG2_NAME);
        Assert.assertEquals(ag2, agOfNameAG2);
    }

}
