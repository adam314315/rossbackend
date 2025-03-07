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
package fr.cnes.regards.framework.test.integration;

import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.util.StringUtils;

/**
 * An utility class for field documentation.
 *
 * @author Marc Sordi
 */
public class ConstrainedFields {

    private final ConstraintDescriptions constraintDescriptions;

    public ConstrainedFields(Class<?> input) {
        this.constraintDescriptions = new ConstraintDescriptions(input);
    }

    /**
     * Field doc tool
     *
     * @param path        payload and property path
     * @param description description
     */
    public FieldDescriptor withPath(String path, String description) {
        return withPath(path, path, description, null);
    }

    /**
     * Field doc tool
     *
     * @param payloadPath  payload path
     * @param propertyPath property path (in POJO)
     * @param description  description
     */
    public FieldDescriptor withPath(String payloadPath, String propertyPath, String description) {
        return withPath(payloadPath, propertyPath, description, null);
    }

    /**
     * Field doc tool
     *
     * @param payloadPath      payload path
     * @param propertyPath     property path (in POJO)
     * @param description      description
     * @param extraConstraints extra doc for constraints
     */
    public FieldDescriptor withPath(String payloadPath,
                                    String propertyPath,
                                    String description,
                                    String extraConstraints) {
        return withPath(null, payloadPath, propertyPath, description, extraConstraints);
    }

    /**
     * Field doc tool
     *
     * @param payloadPrefixPath payload path prefix
     * @param payloadPath       payload path
     * @param propertyPath      property path (in POJO)
     * @param description       description
     * @param extraConstraints  extra doc for constraints
     */
    public FieldDescriptor withPath(String payloadPrefixPath,
                                    String payloadPath,
                                    String propertyPath,
                                    String description,
                                    String extraConstraints) {
        StringBuilder constraints = new StringBuilder(StringUtils.collectionToDelimitedString(constraintDescriptions.descriptionsForProperty(
            propertyPath), ", "));
        if (extraConstraints != null) {
            if (constraints.length() > 0) {
                if (!constraints.toString().endsWith(". ")) {
                    if (constraints.charAt(constraints.length() - 1) == '.') {
                        constraints.append(" ");
                    } else {
                        constraints.append(". ");
                    }
                }
            }
            constraints.append(extraConstraints);
        }

        String fullPayloadPath;
        if (payloadPrefixPath != null) {
            fullPayloadPath = payloadPrefixPath + "." + payloadPath;
        } else {
            fullPayloadPath = payloadPath;
        }

        return PayloadDocumentation.fieldWithPath(fullPayloadPath)
                                   .description(description)
                                   .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                         .value(constraints));
    }

    /**
     * Mark every sub property to the provided path to ignore
     *
     * @param payloadPath payload path
     */
    public FieldDescriptor ignoreSubsectionWithPath(String payloadPath) {
        return PayloadDocumentation.subsectionWithPath(payloadPath).ignored();
    }
}
