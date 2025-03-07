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
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marc Sordi
 */
public class ModelIT extends AbstractModelIT {

    /**
     * Logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(ModelIT.class);

    /**
     * Create a model, attach an attribute model and try to retrieve
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_MOD_010")
    @Purpose("Create a model of type COLLECTION - DOCUMENT, DATA or DATASET are also available -")
    public void createModel() {
        final Model model = createModel("MISSION", "Scientist mission collection", EntityType.COLLECTION);

        // Create attribute
        AttributeModel attModel = new AttributeModelBuilder("NAME", PropertyType.STRING, "ForTests").build();
        attModel = saveAttribute(attModel);

        // Create model attribute
        final ModelAttrAssoc modelAtt = new ModelAttrAssoc(attModel, model);
        modelAttributeRepository.save(modelAtt);

        // Retrieve all model attributes
        final Iterable<ModelAttrAssoc> directAtts = modelAttributeRepository.findByModelId(model.getId());
        Assert.assertEquals(1, Iterables.size(directAtts));
    }

    // TODO try to delete a non empty model
}
