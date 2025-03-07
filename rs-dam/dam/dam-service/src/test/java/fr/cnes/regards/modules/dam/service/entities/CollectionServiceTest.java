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
package fr.cnes.regards.modules.dam.service.entities;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.dam.dao.entities.IAbstractEntityRepository;
import fr.cnes.regards.modules.dam.dao.entities.IAbstractEntityRequestRepository;
import fr.cnes.regards.modules.dam.dao.entities.ICollectionRepository;
import fr.cnes.regards.modules.dam.dao.entities.IDeletedEntityRepository;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import fr.cnes.regards.modules.dam.service.settings.IDamSettingsService;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.service.IModelAttrAssocService;
import fr.cnes.regards.modules.model.service.IModelService;
import fr.cnes.regards.modules.model.service.validation.IModelFinder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author lmieulet
 * @author Sylvain Vissiere-Guerinet
 */
public class CollectionServiceTest {

    private Model pModel1;

    private Model pModel2;

    private Collection collection1;

    private Collection collection2;

    private Collection collection3;

    private Collection collection4;

    private ICollectionRepository collectionRepositoryMocked;

    private ICollectionService collectionServiceMocked;

    private IAbstractEntityRepository<AbstractEntity<?>> entitiesRepositoryMocked;

    private IDamSettingsService damSettingsService;

    /**
     * initialize the repo before each test
     */
    @SuppressWarnings({ "unchecked" })
    @Before
    public void init() {

        damSettingsService = Mockito.mock(IDamSettingsService.class);
        Mockito.when(damSettingsService.isStoreFiles()).thenReturn(false);

        // populate the repository
        pModel1 = new Model();
        pModel1.setId(1L);
        pModel2 = new Model();
        pModel2.setId(2L);

        collection1 = new Collection(pModel1, "PROJECT", "COL1", "collection1");
        collection1.setId(1L);
        collection2 = new Collection(pModel2, "PROJECT", "COL2", "collection2");
        collection2.setId(2L);
        collection3 = new Collection(pModel2, "PROJECT", "COL3", "collection3");
        collection3.setId(3L);
        collection4 = new Collection(pModel2, "PROJECT", "COL4", "collection4");
        collection4.setId(4L);
        collection1.addTags(collection2.getIpId().toString());
        collection2.addTags(collection1.getIpId().toString());

        // create a mock repository
        collectionRepositoryMocked = Mockito.mock(ICollectionRepository.class);
        Mockito.when(collectionRepositoryMocked.findById(collection1.getId())).thenReturn(Optional.of(collection1));
        Mockito.when(collectionRepositoryMocked.findById(collection2.getId())).thenReturn(Optional.of(collection2));
        Mockito.when(collectionRepositoryMocked.findById(collection3.getId())).thenReturn(Optional.of(collection3));

        entitiesRepositoryMocked = Mockito.mock(IAbstractEntityRepository.class);
        List<AbstractEntity<?>> findByTagsValueCol2IpId = new ArrayList<>();
        findByTagsValueCol2IpId.add(collection1);
        Mockito.when(entitiesRepositoryMocked.findByTags(collection2.getIpId().toString()))
               .thenReturn(findByTagsValueCol2IpId);
        Mockito.when(entitiesRepositoryMocked.findById(collection1.getId())).thenReturn(Optional.of(collection1));
        Mockito.when(entitiesRepositoryMocked.findById(collection2.getId())).thenReturn(Optional.of(collection2));
        Mockito.when(entitiesRepositoryMocked.findById(collection3.getId())).thenReturn(Optional.of(collection3));

        @SuppressWarnings("unused")
        IModelAttrAssocService pModelAttributeService = Mockito.mock(IModelAttrAssocService.class);
        IModelService pModelService = Mockito.mock(IModelService.class);
        IDeletedEntityRepository deletedEntityRepositoryMocked = Mockito.mock(IDeletedEntityRepository.class);

        IPublisher publisherMocked = Mockito.mock(IPublisher.class);

        IRuntimeTenantResolver runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn("Tenant");

        collectionServiceMocked = new CollectionService(Mockito.mock(IModelFinder.class),
                                                        entitiesRepositoryMocked,
                                                        pModelService,
                                                        Mockito.mock(IPluginService.class),
                                                        damSettingsService,
                                                        deletedEntityRepositoryMocked,
                                                        collectionRepositoryMocked,
                                                        null,
                                                        null,
                                                        publisherMocked,
                                                        runtimeTenantResolver,
                                                        Mockito.mock(IAbstractEntityRequestRepository.class));
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_COL_010")
    @Requirement("REGARDS_DSL_SYS_ARC_400")
    @Purpose("REGARDS allows to create a collection based on a model - The collection identifier is an URN")
    public void createCollection() throws ModuleException, IOException {
        Mockito.when(collectionRepositoryMocked.save(collection2)).thenReturn(collection2);
        final Collection collection = collectionServiceMocked.create(collection2);
        Assert.assertEquals(collection2, collection);
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_450")
    @Purpose("The URN identifier of a collection is uniq")
    public void collectionUrnUnicity() throws ModuleException, IOException {

        String collName = "collection1";

        Collection coll1 = new Collection(pModel1, "PROJECT", collName, collName);
        Collection coll2 = new Collection(pModel1, "PROJECT", collName, collName);

        Assert.assertNotNull(coll1);
        Assert.assertNotNull(coll2);
        Assert.assertNotNull(coll1.getIpId());
        Assert.assertNotNull(coll2.getIpId());
        Assert.assertNotEquals(coll1.getIpId(), coll2.getIpId());
    }

}
