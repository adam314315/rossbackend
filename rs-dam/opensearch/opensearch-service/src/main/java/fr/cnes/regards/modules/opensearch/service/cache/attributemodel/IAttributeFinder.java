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
package fr.cnes.regards.modules.opensearch.service.cache.attributemodel;

import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;

import java.util.Optional;
import java.util.Set;

/**
 * Retrieve (fake) static or (real) dynamic attributes based on open search parameter name.
 *
 * @author Marc Sordi
 */
public interface IAttributeFinder {

    /**
     * Return the {@link AttributeModel} related to the passed name
     *
     * @param name open search parameter name
     * @return the {@link AttributeModel}
     * @throws OpenSearchUnknownParameter if parameter name cannot be mapped to an attribute
     */
    AttributeModel findByName(String name) throws OpenSearchUnknownParameter;

    /**
     * Return an Optional of {@link AttributeModel} related to the passed name
     * Does not return any exception
     *
     * @param name open search parameter name
     * @return the Optional of {@link AttributeModel}
     */
    Optional<AttributeModel> findByNameOptional(String name);

    /**
     * @param type type filter
     * @return the list of {@link AttributeModel} with specified type
     * @throws OpenSearchUnknownParameter if no parameter can be found with specified type
     */
    Set<AttributeModel> findByType(PropertyType type) throws OpenSearchUnknownParameter;

    /**
     * @return the list of {@link AttributeModel} registered for the current tenant
     */
    Set<AttributeModel> findAll();

    /**
     * Return the smaller distinct path of the given attribute by removing if possible "feature", "properties" and
     * fragment names.
     *
     * @param attribute {@link AttributeModel} to find smaller path name.
     * @return {@link String} smaller path name
     */
    public String findName(AttributeModel attribute);

    /**
     * Refresh finder cache for specified tenant
     */
    public void refresh(String tenant);
}
