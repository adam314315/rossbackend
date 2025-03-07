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
package fr.cnes.regards.modules.indexer.domain.facet.adapters.gson;

import com.google.common.collect.Range;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import fr.cnes.regards.modules.indexer.domain.facet.NumericFacet;
import fr.cnes.regards.modules.indexer.domain.facet.StringFacet;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Simplify the serialization of {@link StringFacet#valueMap}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class NumericFacetSerializer implements JsonSerializer<NumericFacet> {

    @Override
    public JsonElement serialize(NumericFacet src, Type srcType, JsonSerializationContext context) {
        return context.serialize(new AdaptedFacet(src));
    }

    /**
     * A POJO describing the adapted shape
     *
     * @author Xavier-Alexandre Brochard
     */
    private static class AdaptedFacet {

        private final String attributeName;

        private final List<AdaptedFacetValue> values;

        public AdaptedFacet(NumericFacet facet) {
            super();
            attributeName = facet.getAttributeName();
            values = facet.getValues()
                          .entrySet()
                          .stream()
                          .map(entry -> new AdaptedFacetValue(entry, facet.getAttributeName()))
                          .collect(Collectors.toList());
        }

    }

    /**
     * A POJO describing the shape of a facet value
     *
     * @author Xavier-Alexandre Brochard
     */
    private static class AdaptedFacetValue {

        private static final String OPENSEARCH_WILDCARD = "*";

        private final String lowerBound;

        private final String upperBound;

        private final Long count;

        private final String openSearchQuery;

        public AdaptedFacetValue(Entry<Range<Double>, Long> entry, String attributeName) {
            Range<Double> key = entry.getKey();
            if (key.hasLowerBound()) {
                lowerBound = entry.getKey().lowerEndpoint().toString();
            } else {
                // lowerBound = String.valueOf(Double.NEGATIVE_INFINITY);
                // Directly build openSearch lower bound
                lowerBound = OPENSEARCH_WILDCARD;
            }
            if (key.hasUpperBound()) {
                upperBound = entry.getKey().upperEndpoint().toString();
            } else {
                // upperBound = String.valueOf(Double.POSITIVE_INFINITY);
                // Directly build openSearch lower bound
                upperBound = OPENSEARCH_WILDCARD;
            }
            count = entry.getValue();
            if (lowerBound.equals(upperBound)) {
                // In case value is negative, \ the -
                String value = (lowerBound.startsWith("-") ? "\\" + lowerBound : lowerBound);
                openSearchQuery = attributeName + ":" + value;
            } else {
                openSearchQuery = attributeName + ":[" + lowerBound + " TO " + upperBound + "}";
            }
        }

    }

}
