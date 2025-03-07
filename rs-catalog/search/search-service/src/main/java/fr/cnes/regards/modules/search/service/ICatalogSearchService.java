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
package fr.cnes.regards.modules.search.service;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.domain.ParsedDateHistogramResponse;
import fr.cnes.regards.modules.search.domain.PropertyBound;
import fr.cnes.regards.modules.search.domain.plugin.CollectionWithStats;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catalog search service interface. Service façade to DAM search module (directly included by catalog).
 *
 * @author Xavier-Alexandre Brochard
 * @author oroussel
 */
public interface ICatalogSearchService {

    /**
     * Perform a business request on specified entity type
     *
     * @param criterion business criterions
     * @param searchKey the search key containing the search type and the result type
     * @param facets    applicable facets, may be <code>null</code>
     * @param pageable  pagination properties
     * @return the page of elements matching the criterions
     */
    <S, R extends IIndexable> FacetPage<R> search(ICriterion criterion,
                                                  SearchKey<S, R> searchKey,
                                                  List<String> facets,
                                                  Pageable pageable) throws SearchException, OpenSearchUnknownParameter;

    /**
     * Same as below but using {@link SearchType}
     */
    <R extends IIndexable> FacetPage<R> search(ICriterion criterion,
                                               SearchType searchType,
                                               List<String> facets,
                                               Pageable pageable) throws SearchException, OpenSearchUnknownParameter;

    /**
     * Compute summary for given request
     *
     * @param criterion business criterions
     * @param searchKey search key
     * @param dataset   restriction to a specified dataset
     * @param dataTypes file types on which to compute summary
     * @return summary
     */
    DocFilesSummary computeDatasetsSummary(ICriterion criterion,
                                           SimpleSearchKey<DataObject> searchKey,
                                           UniformResourceName dataset,
                                           List<DataType> dataTypes) throws SearchException;

    /**
     * Same as below but using {@link SearchType}
     */
    DocFilesSummary computeDatasetsSummary(ICriterion criterion,
                                           SearchType searchType,
                                           UniformResourceName dataset,
                                           List<DataType> dataTypes);

    /**
     * Retrieve entity
     *
     * @param urn identifier of the entity we are looking for
     * @param <E> concrete type of AbstractEntity
     * @return the entity
     */
    <E extends AbstractEntity<?>> E get(UniformResourceName urn)
        throws EntityOperationForbiddenException, EntityNotFoundException;

    /**
     * Retrieve property values for specified property name
     *
     * @param criterion    business criterions
     * @param searchKey    the search key containing the search type and the result type
     * @param propertyPath target propertu
     * @param maxCount     maximum result count
     * @param partialText  text that property should contains (can be null)
     */
    <T extends IIndexable> List<String> retrieveEnumeratedPropertyValues(ICriterion criterion,
                                                                         SearchKey<T, T> searchKey,
                                                                         String propertyPath,
                                                                         int maxCount,
                                                                         String partialText);

    /**
     * Retrieve property values for specified property name
     *
     * @param criterion    business criterions
     * @param searchType   the search type containing the search type and the result type
     * @param propertyPath target propertu
     * @param maxCount     maximum result count
     * @param partialText  text that property should contains (can be null)
     */
    List<String> retrieveEnumeratedPropertyValues(ICriterion criterion,
                                                  SearchType searchType,
                                                  String propertyPath,
                                                  int maxCount,
                                                  String partialText);

    /**
     * Retrieve statistics for given attribute from a search context with search criterions and searcType.
     *
     * @param criterion  {@link ICriterion}s for search context
     * @param searchType {@link SearchType} for searc context
     * @param attributes {@link AttributeModel}s to retrieve statistics on.
     * @return {@link QueryableAttribute}s for each attribute
     */
    List<Aggregation> retrievePropertiesStats(ICriterion criterion,
                                              SearchType searchType,
                                              Collection<QueryableAttribute> attributes);

    /**
     * Get collection by urn and get its dataobjects statistics
     *
     * @param urn        {@link UniformResourceName} If of the collection we want to get
     * @param searchType {@link SearchType} for search context
     * @param attributes {@link AttributeModel} to retrieve statistics on
     * @return {@link CollectionWithStats}
     * @throws EntityOperationForbiddenException according to {@link #get(UniformResourceName)}
     * @throws EntityNotFoundException           according to {@link #get(UniformResourceName)}
     */
    CollectionWithStats getCollectionWithDataObjectsStats(UniformResourceName urn,
                                                          SearchType searchType,
                                                          Collection<QueryableAttribute> attributes)
        throws SearchException, EntityOperationForbiddenException, EntityNotFoundException;

    /**
     * Retrieve {@link PropertyBound}s for each property given and {@link ICriterion} search.
     *
     * @return @link PropertyBound}s
     */
    List<PropertyBound<?>> retrievePropertiesBounds(Set<String> propertyNames, ICriterion parse, SearchType type);

    /**
     * Retrieve date histogram for a selection of data objects
     *
     * @param searchKey             identify target entity types
     * @param propertyPath          property path
     * @param criterion             {@link ICriterion}s for search context
     * @param dateHistogramInterval date histogram interval
     * @param from                  histogram start datetime
     * @param to                    histogram end datetime
     * @param zoneId                {@link ZoneId} for search context
     * @return {@link ParsedDateHistogram}
     */
    <T extends IIndexable> ParsedDateHistogram getDateHistogram(SearchKey<?, T> searchKey,
                                                                String propertyPath,
                                                                ICriterion criterion,
                                                                DateHistogramInterval dateHistogramInterval,
                                                                OffsetDateTime from,
                                                                OffsetDateTime to,
                                                                ZoneId zoneId);

    /**
     * Retrieve date histograms with same settings for multiple subsets of data objects
     *
     * @param searchKey             identify target entity types
     * @param propertyPath          property path
     * @param criteria              Map of {@link ICriterion}s representing subsets of data objects
     * @param dateHistogramInterval date histogram interval
     * @param from                  histogram start datetime
     * @param to                    histogram end datetime
     * @param zoneId                {@link ZoneId} for search context
     * @return {@link ParsedDateHistogramResponse}
     */
    <T extends IIndexable> Map<String, ParsedDateHistogramResponse> getDateHistograms(SearchKey<?, T> searchKey,
                                                                                      String propertyPath,
                                                                                      Map<String, ICriterion> criteria,
                                                                                      DateHistogramInterval dateHistogramInterval,
                                                                                      OffsetDateTime from,
                                                                                      OffsetDateTime to,
                                                                                      ZoneId zoneId);

    /**
     * Retrieve products metadata from their urns
     */
    List<DataObject> searchByUrnIn(Set<UniformResourceName> urns)
        throws SearchException, OpenSearchUnknownParameter, EntityOperationForbiddenException;
}
