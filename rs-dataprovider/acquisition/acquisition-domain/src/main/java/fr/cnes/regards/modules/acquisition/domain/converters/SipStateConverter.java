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
package fr.cnes.regards.modules.acquisition.domain.converters;

import fr.cnes.regards.modules.acquisition.domain.SipStateManager;
import fr.cnes.regards.modules.ingest.dto.ISipState;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter for extended SIP state
 *
 * @author Marc Sordi
 */
@Converter(autoApply = true)
public class SipStateConverter implements AttributeConverter<ISipState, String> {

    @Override
    public String convertToDatabaseColumn(ISipState attribute) {
        return attribute == null ? null : attribute.getName();
    }

    @Override
    public ISipState convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SipStateManager.fromName(dbData);
    }

}
