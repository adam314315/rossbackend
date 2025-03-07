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
package fr.cnes.regards.framework.jpa.converter;

import com.google.common.collect.Sets;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Allow to convert a {@link Set} of String to a simple String which each value is separated by a comma
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Converter
public class SetStringCsvConverter implements AttributeConverter<Set<String>, String> {

    /**
     * Delimiter used
     */
    private static final String DELIMITER = ",";

    /**
     * @return converted set to string for the database
     */
    @Override
    public String convertToDatabaseColumn(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return null;
        }
        StringJoiner sj = new StringJoiner(DELIMITER);
        for (String entry : set) {
            sj.add(entry);
        }
        return sj.toString();

    }

    /**
     * @return converted string from database to a set
     */
    @Override
    public Set<String> convertToEntityAttribute(String multiValueString) {
        Set<String> result = Sets.newHashSet();
        if (multiValueString == null) {
            return result;
        }
        String[] entries = multiValueString.split(DELIMITER);
        Collections.addAll(result, entries);
        return result;
    }

}
