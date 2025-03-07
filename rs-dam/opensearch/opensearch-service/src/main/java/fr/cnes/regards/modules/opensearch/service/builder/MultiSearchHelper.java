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
package fr.cnes.regards.modules.opensearch.service.builder;

import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;

import java.util.HashSet;
import java.util.Set;

/**
 * Help to define target fields for multi search queries
 *
 * @author Marc SORDI
 */
public final class MultiSearchHelper {

    private MultiSearchHelper() {
        // Nothing to do
    }

    /**
     * This method return fields than can match value type.<br/>
     * At least attributes of type {@link PropertyType#STRING} is selected.
     */
    public static Set<AttributeModel> discoverFields(final IAttributeFinder finder, String value)
        throws OpenSearchUnknownParameter {
        // TODO test with number, date, etc. so we can add fields with this type. Value has to be "castable" to this type.
        Set<AttributeModel> result = new HashSet<>();
        result.addAll(finder.findByType(PropertyType.STRING));
        result.addAll(finder.findByType(PropertyType.STRING_ARRAY));
        // Remove all attributes not indexed (not indexed => not searchable)
        result.removeIf(attributeModel -> !attributeModel.isIndexed());
        return result;
    }
}
