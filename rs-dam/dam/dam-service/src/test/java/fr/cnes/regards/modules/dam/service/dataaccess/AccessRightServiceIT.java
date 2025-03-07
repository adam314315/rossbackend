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
package fr.cnes.regards.modules.dam.service.dataaccess;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.dam.dao.dataaccess.IAccessGroupRepository;
import fr.cnes.regards.modules.dam.dao.dataaccess.IAccessRightRepository;
import fr.cnes.regards.modules.dam.dao.entities.IDatasetRepository;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessright.*;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.service.entities.IDatasetService;
import fr.cnes.regards.modules.model.dao.IModelRepository;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.service.IModelService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Marc SORDI
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=dam_access_rights" },
                    locations = "classpath:es.properties")
public class AccessRightServiceIT extends AbstractMultitenantServiceIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessRightServiceIT.class);

    @Autowired
    private IModelService modelService;

    @Autowired
    private IDatasetService datasetService;

    @Autowired
    private IAccessGroupService groupService;

    @Autowired
    private IAccessRightService accessRightService;

    @Autowired
    private IAccessRightRepository accessRightRepo;

    @Autowired
    private IAccessGroupRepository accessGroupRepo;

    @Autowired
    private IDatasetRepository datasetRepo;

    @Autowired
    private IModelRepository modelRepo;

    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @Test
    public void createAndUpdate() throws ModuleException {

        // Create model
        Model model = modelService.createModel(Model.build("MODEL1", "DS1 description", EntityType.DATASET));

        // Create dataset
        Dataset dataset = new Dataset(model, getDefaultTenant(), "DS1", "DS1 label");
        datasetService.create(dataset);

        // Create group
        AccessGroup group = new AccessGroup("AR1");
        groupService.createAccessGroup(group);

        // Create access right
        QualityFilter filter = new QualityFilter(10, 1, QualityLevel.ACCEPTED);
        AccessRight ar = new AccessRight(filter, AccessLevel.FULL_ACCESS, dataset, group);
        ar.setFileAccessLevel(FileAccessLevel.NO_ACCESS);
        accessRightService.createAccessRight(ar);

        // Update access right
        accessRightService.updateAccessRight(ar.getId(), ar);
    }

    @After
    public void cleanUp() {
        accessRightRepo.deleteAll();
        accessGroupRepo.deleteAll();
        datasetRepo.deleteAll();
        modelRepo.deleteAll();
    }

    //    @Test
    //    public void update() throws ModuleException {
    //
    //        QualityFilter filter = new QualityFilter(10, 1, QualityLevel.ACCEPTED);
    //        Dataset dataset = new Dataset();
    //        dataset.setId(52L);
    //        AccessGroup group = new AccessGroup();
    //        group.setId(102L);
    //
    //        AccessRight ar = new AccessRight(filter, AccessLevel.FULL_ACCESS, dataset, group);
    //        accessRightService.updateAccessRight(52L, ar);
    //    }

}
