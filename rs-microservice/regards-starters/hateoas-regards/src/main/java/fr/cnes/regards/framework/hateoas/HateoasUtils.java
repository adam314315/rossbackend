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
package fr.cnes.regards.framework.hateoas;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * General utility methods for working with {@link ResponseEntity}s and {@link EntityModel}s.
 *
 * @author xbrochar
 */
public final class HateoasUtils {

    /**
     * Utility classes must not have public or default constructor
     */
    private HateoasUtils() {

    }

    /**
     * Wraps an object in a {@link EntityModel}.
     *
     * @param toWrap The resource to wrap
     * @param links  The resource's links
     * @param <T>    The resource type
     * @return The wrap resource
     */
    public static <T> EntityModel<T> wrap(T toWrap, Link... links) {
        return EntityModel.of(toWrap, links);
    }

    /**
     * Wraps an list of objects in a list {@link EntityModel}s.
     *
     * @param toWrap The resource to wrap
     * @param <T>    The resource type
     * @return The wrap resource
     */
    public static <T> List<EntityModel<T>> wrapList(List<T> toWrap) {
        List<EntityModel<T>> asResources = new ArrayList<>();
        for (T item : toWrap) {
            asResources.add(EntityModel.of(item));
        }
        return asResources;
    }

    /**
     * Wraps a collection of objects in a collection of {@link EntityModel}s.
     *
     * @param toWrap The resource to wrap
     * @param <T>    The resource type
     * @return The wrap resource
     */
    public static <T> Collection<EntityModel<T>> wrapCollection(Collection<T> toWrap) {
        Collection<EntityModel<T>> asResources = new ArrayList<>();
        for (T item : toWrap) {
            asResources.add(EntityModel.of(item));
        }
        return asResources;
    }

    /**
     * Unwraps a {@link EntityModel}.
     *
     * @param wrapped The wrapped resource
     * @param <T>     The wrapped resource type
     * @return The unwrapped resource
     */
    public static <T> T unwrap(EntityModel<T> wrapped) {
        T content = null;
        if (wrapped != null) {
            content = wrapped.getContent();
        }
        return content;
    }

    /**
     * Unwraps a {@link List} of {@link EntityModel}s.
     *
     * @param wrapped A list of resources
     * @param <T>     The wrapped resource type
     * @return The unwrapped list of resources
     */
    public static <T> List<T> unwrapList(List<EntityModel<T>> wrapped) {
        List<T> result = new ArrayList<>();
        if (wrapped != null) {
            for (EntityModel<T> entityModel : wrapped) {
                if (entityModel != null) {
                    result.add(entityModel.getContent());
                }
            }
        }
        return result;
    }

    /**
     * Unwraps a {@link Collection} of {@link EntityModel}s.
     *
     * @param wrapped A collection of resources
     * @param <T>     The wrapped resource type
     * @return The unwrapped list of resources
     */
    public static <T> List<T> unwrapCollection(Collection<EntityModel<T>> wrapped) {
        return unwrapList(new ArrayList<>(wrapped));
    }

    /**
     * Transforms a collection to a paged resources of resource(without links) of one page with all the elements.
     *
     * @param elements elements to wrap
     * @return PagedModel<Resource < ?>> of one page containing all base elements
     */
    public static <T> PagedModel<EntityModel<T>> wrapToPagedResources(Collection<T> elements) {
        List<EntityModel<T>> elementResources = elements.stream().map(EntityModel::of).collect(Collectors.toList());
        return PagedModel.of(elementResources, new PageMetadata(elements.size(), 0, elements.size()));
    }

    /**
     * Retrieve all elements from a hateoas paginated endpoint
     *
     * @param pageSize number of elements to retrieve by page
     * @param request  request to execute for each page
     * @return {@link List} of results
     */
    public static <T> List<T> retrieveAllPages(int pageSize,
                                               Function<Pageable, ResponseEntity<PagedModel<EntityModel<T>>>> request) {
        List<T> results = new ArrayList<>();
        PagedModel<EntityModel<T>> page = null;
        PageMetadata metadata = null;
        ResponseEntity<PagedModel<EntityModel<T>>> response;
        Pageable pageable = PageRequest.of(0, pageSize);
        do {
            response = request.apply(pageable);
            if (response != null && response.getStatusCode().equals(HttpStatus.OK)) {
                page = response.getBody();
                if (page != null) {
                    results.addAll(HateoasUtils.unwrapList(new ArrayList<>(page.getContent())));
                    metadata = page.getMetadata();
                }
            }
            pageable = pageable.next();
        } while (page != null && metadata != null && results.size() < metadata.getTotalElements());
        return results;
    }

}
