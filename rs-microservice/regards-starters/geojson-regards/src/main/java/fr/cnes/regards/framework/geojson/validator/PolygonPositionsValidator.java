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
package fr.cnes.regards.framework.geojson.validator;

import fr.cnes.regards.framework.geojson.coordinates.PolygonPositions;
import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.geometry.Polygon;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validate {@link Polygon} coordinate structure
 *
 * @author Marc Sordi
 */
public class PolygonPositionsValidator implements ConstraintValidator<PolygonPositionsConstraints, PolygonPositions> {

    @Override
    public void initialize(PolygonPositionsConstraints constraintAnnotation) {
        // Nothing to do
    }

    @Override
    public boolean isValid(PolygonPositions positions, ConstraintValidatorContext context) {
        if ((positions == null) || positions.isEmpty()) {
            // At least exterior ring is required
            return false;
        }
        for (Positions linearRing : positions) {
            if (!linearRing.isLinearRing()) {
                return false;
            }
        }
        return true;
    }

}
