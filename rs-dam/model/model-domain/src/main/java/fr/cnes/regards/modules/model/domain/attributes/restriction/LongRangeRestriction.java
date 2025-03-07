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
package fr.cnes.regards.modules.model.domain.attributes.restriction;

import fr.cnes.regards.modules.model.domain.attributes.restriction.validator.CheckLongRange;
import fr.cnes.regards.modules.model.domain.schema.LongRange;
import fr.cnes.regards.modules.model.domain.schema.LongRange.Max;
import fr.cnes.regards.modules.model.domain.schema.LongRange.Min;
import fr.cnes.regards.modules.model.domain.schema.Restriction;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;

/**
 * Manage date format restriction for attribute of type :
 * <ul>
 * <li>{@link PropertyType#LONG}</li>
 * <li>{@link PropertyType#LONG_ARRAY}</li>
 * <li>{@link PropertyType#LONG_INTERVAL}</li>
 * </ul>
 *
 * @author oroussel
 */
@CheckLongRange
@Entity
@DiscriminatorValue("LONG_RANGE")
public class LongRangeRestriction extends AbstractRestriction {

    /**
     * Minimum possible value (included)
     * PEGI 18 : same column used for LongRangeRestriction and IntegerRangeRestriction
     */
    @Column(name = "mini")
    @NotNull
    private Long min;

    /**
     * Maximun possible value (included)
     */
    @Column(name = "maxi")
    @NotNull
    private Long max;

    /**
     * Minimum possible value (excluded)
     */
    @Column(name = "mini_excluded")
    private boolean minExcluded = false;

    /**
     * Maximum possible value (excluded)
     */
    @Column(name = "maxi_excluded")
    private boolean maxExcluded = false;

    public LongRangeRestriction() {// NOSONAR
        super();
        type = RestrictionType.LONG_RANGE;
    }

    @Override
    public Boolean supports(PropertyType pPropertyType) {
        return PropertyType.LONG.equals(pPropertyType)
               || PropertyType.LONG_ARRAY.equals(pPropertyType)
               || PropertyType.LONG_INTERVAL.equals(pPropertyType);
    }

    public Long getMin() {
        return min;
    }

    public void setMin(Long pMin) {
        min = pMin;
    }

    public Long getMax() {
        return max;
    }

    public void setMax(Long pMax) {
        max = pMax;
    }

    public boolean isMinExcluded() {
        return minExcluded;
    }

    public void setMinExcluded(boolean pMinExcluded) {
        minExcluded = pMinExcluded;
    }

    public boolean isMaxExcluded() {
        return maxExcluded;
    }

    public void setMaxExcluded(boolean pMaxExcluded) {
        maxExcluded = pMaxExcluded;
    }

    @Override
    public Restriction toXml() {

        final Restriction restriction = new Restriction();
        final LongRange irr = new LongRange();

        Max xmlMax = new Max();
        xmlMax.setValue(max);
        xmlMax.setExcluded(maxExcluded);
        irr.setMax(xmlMax);

        Min xmlMin = new Min();
        xmlMin.setValue(min);
        xmlMin.setExcluded(minExcluded);
        irr.setMin(xmlMin);

        restriction.setLongRange(irr);
        return restriction;
    }

    @Override
    public void fromXml(Restriction pXmlElement) {
        final LongRange ir = pXmlElement.getLongRange();
        Max xmlMax = ir.getMax();
        setMax(xmlMax.getValue());
        setMaxExcluded(xmlMax.isExcluded());
        Min xmlMin = ir.getMin();
        setMin(xmlMin.getValue());
        setMinExcluded(xmlMin.isExcluded());
    }

}
