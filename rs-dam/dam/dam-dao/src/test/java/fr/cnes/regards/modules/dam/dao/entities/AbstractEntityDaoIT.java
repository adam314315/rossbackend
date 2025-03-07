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
package fr.cnes.regards.modules.dam.dao.entities;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalIT;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import fr.cnes.regards.modules.model.dao.IModelRepository;
import fr.cnes.regards.modules.model.domain.Model;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test entity DAO + JSONB functions
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=projectdb" })
public class AbstractEntityDaoIT extends AbstractDaoTransactionalIT {

    private static final String TAG_TO_SEARCH = "tag";

    private static final String TAG_TEST = "TEST";

    private static final String TAG_LAST = "LAST";

    @Autowired
    private IAbstractEntityRepository<AbstractEntity<?>> entityRepository;

    @Autowired
    private IModelRepository modelRepository;

    private Model collectionModel;

    private Collection createEntity(Model model, String tenant, String label) {
        Collection entity = new Collection(model, tenant, label, label);
        entity.setCreationDate(OffsetDateTime.now());
        return entity;
    }

    @Before
    public void init() {
        // runtimeTenantResolver.forceTenant(getDefaultTenant());

        collectionModel = Model.build("name", "desc", EntityType.COLLECTION);
        modelRepository.save(collectionModel);
    }

    @Test
    public void testfindByTagsValue() {

        Collection entity1 = createEntity(collectionModel, getDefaultTenant(), "entity1");
        Set<String> entity1Tags = new HashSet<>();
        entity1Tags.add(TAG_TO_SEARCH);
        entity1Tags.add(TAG_TEST);
        entity1Tags.add(TAG_LAST);
        entity1.setTags(entity1Tags);
        entity1 = entityRepository.save(entity1);

        Collection entity2 = createEntity(collectionModel, getDefaultTenant(), "entity2");
        Set<String> entity2Tags = new HashSet<>();
        entity2Tags.add(TAG_TEST);
        entity2Tags.add(TAG_LAST);
        entity2Tags.add(TAG_TO_SEARCH);
        entity2.setTags(entity2Tags);
        entity2 = entityRepository.save(entity2);

        Collection entity3 = createEntity(collectionModel, getDefaultTenant(), "entity3");
        Set<String> entity3Tags = new HashSet<>();
        entity3Tags.add(TAG_TEST);
        entity3Tags.add(TAG_TO_SEARCH);
        entity3Tags.add(TAG_LAST);
        entity3.setTags(entity3Tags);
        entity3 = entityRepository.save(entity3);

        Collection entity4 = createEntity(collectionModel, getDefaultTenant(), "entity4");
        Set<String> entity4Tags = new HashSet<>();
        entity4Tags.add(TAG_TEST);
        entity4Tags.add(TAG_LAST);
        entity4.setTags(entity4Tags);
        entity4 = entityRepository.save(entity4);

        List<AbstractEntity<?>> result = entityRepository.findByTags(TAG_TO_SEARCH);
        Assert.assertTrue(result.contains(entity1));
        Assert.assertTrue(result.contains(entity2));
        Assert.assertTrue(result.contains(entity3));
        Assert.assertFalse(result.contains(entity4));

    }

    @Test
    public void testFindByIpIdIn() {
        Collection entity1 = createEntity(collectionModel, getDefaultTenant(), "entity1");
        entity1 = entityRepository.save(entity1);

        Collection entity2 = createEntity(collectionModel, getDefaultTenant(), "entity2");
        entity2 = entityRepository.save(entity2);

        Collection entity3 = createEntity(collectionModel, getDefaultTenant(), "entity3");
        entity3 = entityRepository.save(entity3);

        Collection entity4 = createEntity(collectionModel, getDefaultTenant(), "entity4");
        entity4 = entityRepository.save(entity4);

        Set<UniformResourceName> ipIds = new HashSet<>();
        ipIds.add(entity1.getIpId());
        ipIds.add(entity3.getIpId());
        ipIds.add(entity2.getIpId());

        List<AbstractEntity<?>> result = entityRepository.findByIpIdIn(ipIds);
        Assert.assertTrue(result.contains(entity1));
        Assert.assertTrue(result.contains(entity2));
        Assert.assertTrue(result.contains(entity3));
        Assert.assertFalse(result.contains(entity4));
    }

    @Test
    public void testFindByIpIdInEmptySet() {

        Collection entity1 = createEntity(collectionModel, getDefaultTenant(), "entity1");
        entity1 = entityRepository.save(entity1);

        Collection entity2 = createEntity(collectionModel, getDefaultTenant(), "entity2");
        entity2 = entityRepository.save(entity2);

        Collection entity3 = createEntity(collectionModel, getDefaultTenant(), "entity3");
        entity3 = entityRepository.save(entity3);

        Collection entity4 = createEntity(collectionModel, getDefaultTenant(), "entity4");
        entity4 = entityRepository.save(entity4);

        Set<UniformResourceName> ipIds = new HashSet<>();

        List<AbstractEntity<?>> result = entityRepository.findByIpIdIn(ipIds);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void findByProviderid() {
        String providerId = "SIPID";
        Collection entity1 = createEntity(collectionModel, getDefaultTenant(), "entity1");
        entity1.setProviderId(providerId);
        entity1 = entityRepository.save(entity1);

        Set<AbstractEntity<?>> entities = entityRepository.findAllByProviderId(providerId);
        Assert.assertEquals(1, entities.size());

        entities = entityRepository.findAllByProviderId("fake");
        Assert.assertEquals(0, entities.size());
    }
}
