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
package fr.cnes.regards.modules.model.service.validation;

import fr.cnes.regards.framework.geojson.AbstractFeature;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import org.springframework.validation.Errors;

import java.util.Set;

/**
 * Validation interface
 *
 * @param <U> {@link AbstractFeature}
 */
public interface IValidationService<U extends AbstractFeature<Set<IProperty<?>>, ?>> {

    /**
     * Validate feature properties regarding the configured data model
     */
    Errors validate(String model, U feature, ValidationMode mode, String objectName);
}
