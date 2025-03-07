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
package fr.cnes.regards.framework.jpa.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * This {@link AttributeConverter} allows to convert a {@link OffsetDateTime} to persist with JPA.
 *
 * @author Christophe Mertz
 * @author oroussel
 */
@Converter(autoApply = true)
public class OffsetDateTimeAttributeConverter implements AttributeConverter<OffsetDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(OffsetDateTime offsetDateTime) {
        return (offsetDateTime == null) ? null :
            // Take UTC date as local date to have an UTC date into database
            // truncate to microseconds because postgres timestamp has a resolution of 1 microsecond
            Timestamp.valueOf(offsetDateTime.truncatedTo(ChronoUnit.MICROS)
                                            .atZoneSameInstant(ZoneOffset.UTC)
                                            .toLocalDateTime());
    }

    @Override
    public OffsetDateTime convertToEntityAttribute(Timestamp timestamp) {
        return (timestamp == null) ? null :
            // Read Timestamp, transform to Local Date as it is an UTC date (which is the case in DB) and transform
            // to OffsetDateTime, keeping it as UTC (ouch !)
            OffsetDateTime.ofInstant(timestamp.toLocalDateTime().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
                          .withOffsetSameInstant(ZoneOffset.UTC);
    }
}