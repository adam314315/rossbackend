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
package fr.cnes.regards.modules.indexer.service;

import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.domain.*;
import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Elasticsearch search service. This service contains all search and get methods. For other methods, check
 * crawler-service and IndexerService class.
 *
 * @author oroussel
 */
public interface ISearchService {

    /**
     * Maximum page size (Elasticsearch constraint)
     * (only used by CatalogSearchService)
     */
    int MAX_PAGE_SIZE = IEsRepository.MAX_RESULT_WINDOW;

    /**
     * Get document by its id (urn)
     */
    <T extends IIndexable> T get(UniformResourceName urn);

    /**
     * Search ordered documents into index following criterion. Some facets are asked for.
     *
     * @param searchKey   identity search key
     * @param pageRequest pagination information ({@link PageRequest}
     * @param criterion   search criterion
     * @param facetsMap   a map of { document property name, facet type }
     * @return a simple page of documents if facet are not asked for, a {@link FacetPage} else
     */
    <T extends IIndexable> FacetPage<T> search(SimpleSearchKey<T> searchKey,
                                               Pageable pageRequest,
                                               ICriterion criterion,
                                               Map<String, FacetType> facetsMap);

    /**
     * Search documents as usual BUT return joined entity whom type is specified into searchKey
     *
     * @param searchKey   the search key. <b>Be careful, the search type must be the type concerned by criterion, result
     *                    class must be joined entity class </b>
     * @param pageRequest pagination information ({@link PageRequest}
     * @param criterion   search criterion on document
     * @param facetsMap   facets, on data, to be calculated
     * @param <S>         entity class on which request is done
     * @param <R>         Joined entity class ("result" type)
     * @return a page of joined entities
     */
    default <S, R extends IIndexable> FacetPage<R> search(JoinEntitySearchKey<S, R> searchKey,
                                                          Pageable pageRequest,
                                                          ICriterion criterion,
                                                          Map<String, FacetType> facetsMap) {
        return search(searchKey, pageRequest, criterion, ICriterion.all(), facetsMap);
    }

    /**
     * Search documents as usual BUT return joined entity whom type is specified into searchKey
     *
     * @param <S>                   entity class on which request is done
     * @param <R>                   Joined entity class ("result" type)
     * @param searchKey             the search key. <b>Be careful, the search type must be the type concerned by criterion, result
     *                              class must be joined entity class </b>
     * @param pageRequest           pagination information ({@link PageRequest}
     * @param criterion             search criterion on document
     * @param searchResultCriterion a filter to be used on the join entity search. Can be null.
     * @param facetsMap             facets, on data, to be calculated
     * @return a page of joined entities
     */
    <S, R extends IIndexable> FacetPage<R> search(JoinEntitySearchKey<S, R> searchKey,
                                                  Pageable pageRequest,
                                                  ICriterion criterion,
                                                  ICriterion searchResultCriterion,
                                                  Map<String, FacetType> facetsMap);

    /**
     * Searching specified page of elements from index giving page size
     *
     * @param searchKey   the search key
     * @param pageRequest page request (use {@link Page#nextPageable()} method for example)
     * @param pValue      value to search
     * @param fields      fields to search on (use '.' for inner objects, ie "attributes.tags"). Wildcards '*' can be used
     *                    too (ie attributes.dataRange.*). <b>Fields types must be consistent with given value type</b>
     * @param <T>         document type
     * @return specified result page
     */
    <T extends IIndexable> Page<T> multiFieldsSearch(SearchKey<T, T> searchKey,
                                                     Pageable pageRequest,
                                                     Object pValue,
                                                     String... fields);

    default <T extends IIndexable> Page<T> multiFieldsSearch(SearchKey<T, T> searchKey,
                                                             int pageSize,
                                                             Object value,
                                                             String... fields) {
        return multiFieldsSearch(searchKey, PageRequest.of(0, pageSize), value, fields);
    }

    default <S, R extends IIndexable> FacetPage<R> search(JoinEntitySearchKey<S, R> searchKey,
                                                          int pageSize,
                                                          ICriterion criterion,
                                                          Map<String, FacetType> facetsMap) {
        return this.search(searchKey, PageRequest.of(0, pageSize), criterion, facetsMap);
    }

    default <T extends IIndexable> Page<T> search(SimpleSearchKey<T> searchKey, int pageSize, ICriterion criterion) {
        return search(searchKey, PageRequest.of(0, pageSize), criterion);
    }

    default <T extends IIndexable> Page<T> search(SimpleSearchKey<T> searchKey,
                                                  Pageable pageRequest,
                                                  ICriterion criterion) {
        return search(searchKey, pageRequest, criterion, null);
    }

    <T extends IIndexable> Aggregations getAggregations(SimpleSearchKey<T> searchKey,
                                                        ICriterion criterion,
                                                        Collection<QueryableAttribute> attributes);

    <T extends IIndexable> Aggregations getAggregationsFor(SearchKey<?, T> searchKey,
                                                           ICriterion criterion,
                                                           Collection<AggregationBuilder> aggregationBuilders,
                                                           int limit);

    <T extends IIndexable> Map<String, AggregationSearchContextResponse> getMultiAggregationsFor(Map<String,
        AggregationSearchContext<T>> searchRequests);

    /**
     * Compute a DocFilesSummary for given request distributing results based on disciminantProperty for given file
     * types
     *
     * @param <T> document type (must be of type IIndexable to be searched and IDocFiles to provide "files" property)
     * @return the compmuted summary
     */
    <T extends IIndexable & IDocFiles> DocFilesSummary computeDataFilesSummary(SearchKey<T, T> searchKey,
                                                                               ICriterion crit,
                                                                               String discriminantProperty,
                                                                               Optional<String> discriminentPropertyInclude,
                                                                               List<DataType> dataTypes);

    /**
     * Search for alphabeticly sorted top maxCount values of given attribute following given request
     */
    <T extends IIndexable> List<String> searchUniqueTopValues(SearchKey<T, T> searchKey,
                                                              ICriterion criterion,
                                                              String attName,
                                                              int maxCount);
}
