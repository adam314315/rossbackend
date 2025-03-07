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

package fr.cnes.regards.modules.model.service.validation.validator.object.restriction;

import com.google.common.collect.Range;
import fr.cnes.regards.modules.model.domain.attributes.restriction.DoubleRangeRestriction;
import fr.cnes.regards.modules.model.dto.properties.adapter.IntervalMapping;
import fr.cnes.regards.modules.model.service.validation.validator.common.restriction.AbstractDoubleRangeValidator;

import java.util.List;
import java.util.Map;

/**
 * Validates Double range given as a map with a {@link DoubleRangeRestriction}
 *
 * @author Thibaud Michaudel
 **/
public class DoubleRangeObjectValidator extends AbstractDoubleRangeValidator {

    public DoubleRangeObjectValidator(DoubleRangeRestriction pRestriction, String pAttributeKey) {
        super(pRestriction, pAttributeKey);
    }

    @Override
    protected Double getDoubleValue(Object pTarget) {
        return (Double) pTarget;
    }

    @Override
    protected Double[] getDoubleArrayValue(Object pTarget) {
        return ((List<Double>) pTarget).toArray(new Double[((List) pTarget).size()]);
    }

    @Override
    protected Range<Double> getDoubleIntervalValue(Object pTarget) {
        Map<String, Double> interval = (Map<String, Double>) pTarget;
        return Range.closed(interval.get(IntervalMapping.RANGE_LOWER_BOUND),
                            interval.get(IntervalMapping.RANGE_UPPER_BOUND));
    }

    @Override
    protected boolean isDouble(Class clazz) {
        return Double.class.isAssignableFrom(clazz);
    }

    @Override
    protected boolean isDoubleArray(Class clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    @Override
    protected boolean isDoubleInterval(Class clazz) {
        return Map.class.isAssignableFrom(clazz);
    }

}
