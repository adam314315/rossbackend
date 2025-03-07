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
package fr.cnes.regards.framework.jpa.utils;

import fr.cnes.regards.framework.jpa.utils.function.NewStringArraySQLFunction;
import fr.cnes.regards.framework.jpa.utils.function.contributor.CustomFunctionsContributor;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * Specification utils
 *
 * @author Léo Mieulet
 */
public class SpecificationUtils {

    public static final String LIKE_CHAR = "%";

    private SpecificationUtils() {
    }

    /**
     * Return a predicate that check if a JSONB string array (ie ["a", "b"]) contains all textSearched items
     *
     * @param attributeRequested the entity field
     * @param textSearched       list of string researched
     * @param cb                 criteria builder
     * @return a Predicate with this constraint
     */
    public static Predicate buildPredicateIsJsonbArrayContainingElements(Path<Object> attributeRequested,
                                                                         List<String> textSearched,
                                                                         CriteriaBuilder cb) {
        // Create an empty array
        @SuppressWarnings("rawtypes") Expression<String[]> allowedValuesConstraint = cb.function(
            NewStringArraySQLFunction.NAME,
            String[].class);
        for (String category : textSearched) {
            // Append to that array every text researched
            allowedValuesConstraint = cb.function("array_append",
                                                  String[].class,
                                                  allowedValuesConstraint,
                                                  cb.function(CustomFunctionsContributor.STRING_LITERAL,
                                                              String.class,
                                                              cb.literal(category)));
        }
        // Check the entity have every text researched
        return cb.isTrue(cb.function(CustomFunctionsContributor.JSONB_EXISTS_ALL,
                                     Boolean.class,
                                     attributeRequested,
                                     allowedValuesConstraint));
    }

    /**
     * Return a predicate that check if a JSONB string array (ie ["a", "b"]) contains at least one of the researched text searched
     *
     * @param attributeRequested the entity field
     * @param textSearched       list of string researched
     * @param cb                 criteria builder
     * @return a Predicate with this constraint
     */
    public static Predicate buildPredicateIsJsonbArrayContainingOneOfElement(Path<Object> attributeRequested,
                                                                             List<String> textSearched,
                                                                             CriteriaBuilder cb) {
        // Create an empty array
        Expression<String[]> allowedValuesConstraint = cb.function(NewStringArraySQLFunction.NAME, String[].class);
        for (String category : textSearched) {
            // Use "array_append" function
            allowedValuesConstraint = cb.function("array_append",
                                                  String[].class,
                                                  allowedValuesConstraint,
                                                  cb.function(CustomFunctionsContributor.STRING_LITERAL,
                                                              String.class,
                                                              cb.literal(category)));
        }
        // Check the entity have every text researched
        return cb.isTrue(cb.function(CustomFunctionsContributor.JSONB_EXISTS_ANY,
                                     Boolean.class,
                                     attributeRequested,
                                     allowedValuesConstraint));
    }

    /**
     * Generate orderBy specification for pageable requests
     *
     * @param page             the page request
     * @param root             root of the entity managed by this specification
     * @param cb               criteria builder
     * @param defaultDirection fallback direction
     * @param defaultAttribute fallback attribute name
     * @return list of order
     */
    public static List<Order> buildOrderBy(Pageable page,
                                           Root<?> root,
                                           CriteriaBuilder cb,
                                           String defaultAttribute,
                                           Sort.Direction defaultDirection) {
        List<Order> orders = new ArrayList<>();
        Sort sort = page.getSortOr(Sort.by(defaultDirection, defaultAttribute));
        for (Sort.Order order : sort) {
            if (order.isAscending()) {
                orders.add(cb.desc(root.get(order.getProperty())));
            } else {
                orders.add(cb.asc(root.get(order.getProperty())));
            }
        }
        return orders;
    }

}
