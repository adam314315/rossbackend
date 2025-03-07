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
package fr.cnes.regards.modules.dam.service.model;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.service.entities.IDatasetService;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.model.service.IAttributeModelService;
import fr.cnes.regards.modules.model.service.IModelAttrAssocService;
import fr.cnes.regards.modules.model.service.IModelService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Test attribute model deletion restrictions
 *
 * @author Marc Sordi
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=deletion" },
                    locations = "classpath:es.properties")
@MultitenantTransactional
public class DeleteAttributeIT extends AbstractMultitenantServiceIT {

    @Autowired
    private IModelService modelService;

    @Autowired
    private IAttributeModelService attModelService;

    @Autowired
    private IModelAttrAssocService assocService;

    @Autowired
    private IDatasetService datasetService;

    private AttributeModel toBeDeleted;

    @Before
    public void setup() throws ModuleException {
        toBeDeleted = new AttributeModelBuilder("TO_BE_DELETED", PropertyType.STRING, "del").build();
        attModelService.createAttribute(toBeDeleted);
    }

    /**
     * An attribute not linked to any model nor entity can be deleted
     */
    @Test
    public void deleteUnlinked() throws ModuleException {
        attModelService.deleteAttribute(toBeDeleted.getId());
    }

    /**
     * An attribute linked to a non used model can be deleted
     */
    @Test
    public void deleteWithLinkToModel() throws ModuleException {
        Model model = Model.build("DEL", "Model for deletion test", EntityType.COLLECTION);
        modelService.createModel(model);
        assocService.bindAttributeToModel(model.getName(), new ModelAttrAssoc(toBeDeleted, model));
        attModelService.deleteAttribute(toBeDeleted.getId());
    }

    /**
     * An attribute linked to a used model cannot be deleted
     */
    @Test(expected = EntityOperationForbiddenException.class)
    public void deleteWithLinkToUsedModel() throws ModuleException {
        Model model = Model.build("DEL", "Model for deletion test", EntityType.DATASET);
        modelService.createModel(model);
        assocService.bindAttributeToModel(model.getName(), new ModelAttrAssoc(toBeDeleted, model));

        Dataset dataset = new Dataset(model, getDefaultTenant(), "DSDEL", "DS label");
        datasetService.create(dataset);

        attModelService.deleteAttribute(toBeDeleted.getId());
    }

    /**
     * An attribute linked to a datasource cannot be deleted
     */
    @Test(expected = EntityOperationForbiddenException.class)
    public void deleteWithLinkToDatasource() throws ModuleException {
        Model datasetModel = Model.build("DSM", "Model for deletion test", EntityType.DATASET);
        modelService.createModel(datasetModel);

        Model dataModel = Model.build("DATA", "Model for deletion test", EntityType.DATA);
        modelService.createModel(dataModel);
        assocService.bindAttributeToModel(dataModel.getName(), new ModelAttrAssoc(toBeDeleted, dataModel));

        Dataset dataset = new Dataset(datasetModel, getDefaultTenant(), "DSDEL", "DS label");
        dataset.setDataModel(dataModel.getName());
        datasetService.create(dataset);

        attModelService.deleteAttribute(toBeDeleted.getId());
    }
}
