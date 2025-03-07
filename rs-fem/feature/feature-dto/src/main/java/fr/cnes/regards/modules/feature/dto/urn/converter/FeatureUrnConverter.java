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
package fr.cnes.regards.modules.feature.dto.urn.converter;

import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter used by Hibernate (see AbstractEntity)
 *
 * @author Kevin Marchois
 */
@Converter(autoApply = true)
public class FeatureUrnConverter implements AttributeConverter<FeatureUniformResourceName, String> {

    @Override
    public String convertToDatabaseColumn(FeatureUniformResourceName urn) {
        if (urn == null) {
            return null;
        }
        return urn.toString();
    }

    @Override
    public FeatureUniformResourceName convertToEntityAttribute(String data) {
        if (data == null) {
            return null;
        }
        return FeatureUniformResourceName.fromString(data);
    }

}
