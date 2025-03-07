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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.gson.AbstractAttributeHelper;
import fr.cnes.regards.modules.model.service.IAttributeModelService;
import fr.cnes.regards.modules.model.service.IModelAttrAssocService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class to retrieve model attributes
 *
 * @author Marc Sordi
 */
@Service
public class DamAttributeHelper extends AbstractAttributeHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DamAttributeHelper.class);

    /**
     * Runtime tenant resolver
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * AttributeModel service to retrieve AttributeModels entities.
     */
    private final IAttributeModelService attributeModelService;

    private final IModelAttrAssocService attributeModelAssocService;

    public DamAttributeHelper(IRuntimeTenantResolver runtimeTenantResolver,
                              IAttributeModelService attributeModelService,
                              IModelAttrAssocService attributeModelAssocService) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.attributeModelService = attributeModelService;
        this.attributeModelAssocService = attributeModelAssocService;
    }

    @Override
    protected List<AttributeModel> doGetAllAttributes() {
        return attributeModelService.getAttributes(null, null, null);
    }

    @Override
    public Set<AttributeModel> getAllCommonAttributes(Collection<String> modelNames) throws ModuleException {
        Set<AttributeModel> commonAttributes = Sets.newHashSet();
        boolean first = true;
        for (String modelName : modelNames) {
            Set<AttributeModel> modelAttributes = attributeModelAssocService.getModelAttrAssocs(modelName)
                                                                            .stream()
                                                                            .map(f -> f.getAttribute())
                                                                            .collect(Collectors.toSet());
            if (first) {
                commonAttributes.addAll(modelAttributes);
            } else {
                commonAttributes = commonAttributes.stream()
                                                   .filter(f -> modelAttributes.contains(f))
                                                   .collect(Collectors.toSet());
            }
            first = false;
        }
        return commonAttributes;
    }
}
