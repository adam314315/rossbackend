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

import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.service.IModelAttrAssocService;
import fr.cnes.regards.modules.model.service.validation.AbstractCacheableModelFinder;
import fr.cnes.regards.modules.model.service.validation.IModelFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Retrieve model attributes in DAM context (directly from database)
 *
 * @author Marc SORDI
 */
@Service
public class DamModelFinder extends AbstractCacheableModelFinder implements IModelFinder {

    @Autowired
    protected IModelAttrAssocService modelAttributeService;

    @Override
    protected List<ModelAttrAssoc> loadAttributesByModel(String modelName) {
        return modelAttributeService.getModelAttrAssocs(modelName);
    }
}
