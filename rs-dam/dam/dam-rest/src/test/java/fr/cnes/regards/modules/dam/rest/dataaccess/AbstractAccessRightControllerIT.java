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
package fr.cnes.regards.modules.dam.rest.dataaccess;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.dam.dao.dataaccess.IAccessGroupRepository;
import fr.cnes.regards.modules.dam.dao.dataaccess.IAccessRightRepository;
import fr.cnes.regards.modules.dam.dao.entities.IDatasetRepository;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroupMapper;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessright.*;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.service.dataaccess.IAccessGroupService;
import fr.cnes.regards.modules.model.dao.IAttributeModelRepository;
import fr.cnes.regards.modules.model.dao.IAttributePropertyRepository;
import fr.cnes.regards.modules.model.dao.IModelAttrAssocRepository;
import fr.cnes.regards.modules.model.dao.IModelRepository;
import fr.cnes.regards.modules.model.domain.Model;
import org.junit.After;
import org.junit.Before;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

/**
 * @author Sylvain Vissiere-Guerinet
 * @author Léo Mieulet
 */
public abstract class AbstractAccessRightControllerIT extends AbstractRegardsIT {

    protected static final String ACCESS_RIGHTS_ERROR_MSG = "Should have been an answer";

    @Autowired
    protected IModelRepository modelRepo;

    @Autowired
    protected IDatasetRepository dsRepo;

    @Autowired
    protected IAccessGroupRepository agRepo;

    protected AccessGroup ag1;

    protected AccessGroup ag2;

    protected final String ag1Name = "AG1";

    protected final String ag2Name = "AG2";

    protected QualityFilter qf;

    protected final AccessLevel al = AccessLevel.FULL_ACCESS;

    protected FileAccessLevel fal;

    protected Dataset ds1;

    protected Dataset ds2;

    protected final String ds1Name = "DS1";

    protected final String ds2Name = "DS2";

    protected AccessRight ar1;

    protected AccessRight ar2;

    protected AccessRight ar3;

    protected ProjectUser projectUser;

    protected final String email = "test@email.com";

    @Autowired
    protected IAccessGroupService agService;

    @Autowired
    private AccessGroupMapper accessGroupMapper;

    @Autowired
    protected IAccessRightRepository arRepo;

    @Autowired
    private IModelRepository modelRepository;

    @Autowired
    private IAttributeModelRepository attModelRepo;

    @Autowired
    private IModelAttrAssocRepository attrModelAssocRepo;

    @Autowired
    private IAttributePropertyRepository attModelPropertyRepo;

    @Autowired
    private IDatasetRepository datasetRepository;

    @Autowired
    protected IRuntimeTenantResolver runtimetenantResolver;

    @Autowired
    protected IProjectUsersClient projectUserClientMock;

    @After
    public void clear() {
        runtimetenantResolver.forceTenant(getDefaultTenant());
        arRepo.deleteAll();
        agRepo.deleteAll();

        datasetRepository.deleteAll();

        attrModelAssocRepo.deleteAll();
        attModelPropertyRepo.deleteAll();
        attModelRepo.deleteAll();
        modelRepository.deleteAll();
        runtimetenantResolver.clearTenant();
    }

    @Before
    public void init() {
        clear();

        runtimetenantResolver.forceTenant(getDefaultTenant());

        OffsetDateTime now = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);
        // Replace stubs by mocks

        qf = new QualityFilter(10, 0, QualityLevel.ACCEPTED);
        fal = FileAccessLevel.NO_ACCESS;

        Model model = Model.build("model1", "desc", EntityType.DATASET);
        model = modelRepo.save(model);

        ds1 = new Dataset(model, "PROJECT", "DS1", ds1Name);
        ds1.setLicence("licence");
        ds1.setCreationDate(now);
        ds1.getGroups().add(ag1Name);
        ds1 = dsRepo.save(ds1);

        ds2 = new Dataset(model, "PROJECT", "DS2", ds2Name);
        ds2.setLicence("licence");
        ds2.setCreationDate(now);
        ds2 = dsRepo.save(ds2);

        ag1 = new AccessGroup(ag1Name);
        ag1 = agRepo.save(ag1);
        ar1 = new AccessRight(qf, al, ds1, ag1);
        ar1.setFileAccessLevel(fal);
        ar1 = arRepo.save(ar1);

        ag2 = new AccessGroup(ag2Name);
        ag2 = agRepo.save(ag2);

        ar2 = new AccessRight(qf, al, ds2, ag2);
        ar2.setFileAccessLevel(fal);
        ar2 = arRepo.save(ar2);

        ar3 = new AccessRight(qf, al, ds1, ag2);
        ar3.setFileAccessLevel(fal);

        projectUser = new ProjectUser();
        projectUser.setEmail(email);
        projectUser.setAccessGroups(Collections.singleton(ag1Name));

        ResponseEntity<EntityModel<ProjectUser>> response = ResponseEntity.ok(EntityModel.of(projectUser));
        Mockito.when(projectUserClientMock.retrieveProjectUser(ArgumentMatchers.any())).thenReturn(response);
        Mockito.when(projectUserClientMock.retrieveProjectUserByEmail(email)).thenReturn(response);
    }

}
