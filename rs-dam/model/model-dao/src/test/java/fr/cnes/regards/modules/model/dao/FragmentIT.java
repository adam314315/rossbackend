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
package fr.cnes.regards.modules.model.dao;

import com.google.common.collect.Iterables;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import jakarta.persistence.PersistenceException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Fragment test
 *
 * @author Marc Sordi
 */
public class FragmentIT extends AbstractModelIT {

    /**
     * Try to delete a non empty fragment
     */
    @Requirement("REGARDS_DSL_DAM_MOD_050")
    @Purpose("Try to delete a non empty fragment")
    @Test(expected = PersistenceException.class)
    public void deleteFragment() {

        final Fragment fragment = Fragment.buildFragment("fragment1", "description fragment 1");

        final AttributeModel attModel = new AttributeModelBuilder("fragment_att1",
                                                                  PropertyType.STRING,
                                                                  "ForTests").setFragment(fragment)
                                                                             .setNoRestriction()
                                                                             .build();
        saveAttribute(attModel);

        final Iterable<AttributeModel> attModels = attModelRepository.findByFragmentId(fragment.getId());
        Assert.assertNotNull(attModels);
        Assert.assertEquals(1, Iterables.size(attModels));

        // Try to remove anyway
        fragmentRepository.delete(fragment);
        entityManager.flush();
        entityManager.clear();
    }
}
