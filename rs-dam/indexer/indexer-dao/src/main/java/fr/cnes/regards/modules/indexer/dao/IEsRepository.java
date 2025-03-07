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
package fr.cnes.regards.modules.indexer.dao;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.indexer.dao.converter.LinkedHashMapToSort;
import fr.cnes.regards.modules.indexer.dao.mapping.AttributeDescription;
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

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Elasticsearch DAO interface
 *
 * @author oroussel
 */
public interface IEsRepository {

    /**
     * ElasticSearch window pagination limit ie only from 0 to 10_000 is permit with a classic search.
     * Outside this window, it is necessary to use scrollable or searchAfter API.
     */
    int MAX_RESULT_WINDOW = 10_000;

    /**
     * Create specified index using default configuration
     *
     * @param index index
     * @return true if acknowledged by Elasticsearch, false otherwise.
     * @deprecated Use {@link #createIndex(String, CreateIndexConfiguration)} instead
     */
    @Deprecated
    boolean createIndex(String index);

    /**
     * Create specified index
     *
     * @param index         index
     * @param configuration the parameters to use
     * @return true if acknowledged by Elasticsearch, false otherwise.
     */
    boolean createIndex(String index, CreateIndexConfiguration configuration);

    /**
     * @see #putMappings(String, Set)
     */
    default boolean putMappings(String index, AttributeDescription... mappings) {
        return putMappings(index, Sets.newHashSet(mappings));
    }

    /**
     * Add mappings for the given index
     */
    boolean putMappings(String index, Set<AttributeDescription> mappings);

    /**
     * Create an alias for an index
     *
     * @param index index name
     * @param alias alias name
     * @return true if acknowledged by Elasticsearch, false otherwise.
     */
    boolean createAlias(String index, String alias);

    boolean setSettingsForBulk(String index);

    boolean unsetSettingsForBulk(String index);

    /**
     * Delete specified index <b>or associated index if an alias is specified</b>
     *
     * @param index index or alias
     * @return true if acknowledged by Elasticsearch, false otherwise.
     */
    boolean deleteIndex(String index);

    /**
     * Does specified index <b>or alias</b>exist ?
     *
     * @param name index or alias name
     * @return true or false
     */
    boolean indexExists(String name);

    /**
     * Create or update a document index specifying index.
     *
     * @param index    index
     * @param document object implementing IIndexable thus needs to provide id and type
     * @return true if created, false otherwise
     */
    boolean save(String index, IIndexable document);

    /**
     * Method only used for tests. Elasticsearch performs refreshes every second. So, il a search is called just after a save, the document will not be available. A manual refresh is necessary (on
     * saveBulkEntities, it is automaticaly called)
     *
     * @param index index to refresh
     */
    void refresh(String index);

    /**
     * Create or update several documents into same index. Errors are logged.
     *
     * @param index          index
     * @param bulkSaveResult bulkSaveResult to use (can be null)
     * @param errorBuffer    errorBuffer filled with documents that cannot be saved
     * @param documents      documents to save (docId and type are mandatory for all of them)
     * @param <T>            parameterized type to avoid array inheritance restriction type definition
     * @return bulk save result
     * @throws IllegalArgumentException If at least one document hasn't its mandatory properties (docId, type, label...).
     */
    @SuppressWarnings("unchecked")
    <T extends IIndexable> BulkSaveResult saveBulk(String index,
                                                   BulkSaveResult bulkSaveResult,
                                                   StringBuilder errorBuffer,
                                                   T... documents) throws IllegalArgumentException;

    /**
     * {@link #saveBulk(String, BulkSaveResult, StringBuilder, IIndexable[])}
     */
    @SuppressWarnings("unchecked")
    default <T extends IIndexable> BulkSaveResult saveBulk(String index, T... documents)
        throws IllegalArgumentException {
        return this.saveBulk(index, null, documents);
    }

    /**
     * {@link #saveBulk(String, BulkSaveResult, StringBuilder, IIndexable[])}
     */
    @SuppressWarnings("unchecked")
    default <T extends IIndexable> BulkSaveResult saveBulk(String index, StringBuilder errorBuffer, T... documents)
        throws IllegalArgumentException {
        return saveBulk(index, null, errorBuffer, documents);
    }

    /**
     * {@link #saveBulk(String, Collection, StringBuilder)}
     */
    default BulkSaveResult saveBulk(String index, Collection<? extends IIndexable> documents)
        throws IllegalArgumentException {
        return this.saveBulk(index, documents.toArray(new IIndexable[documents.size()]));
    }

    /**
     * {@link #saveBulk(String, BulkSaveResult, StringBuilder, IIndexable[])}
     */
    default BulkSaveResult saveBulk(String index,
                                    BulkSaveResult bulkSaveResult,
                                    Collection<? extends IIndexable> documents,
                                    StringBuilder errorBuffer) throws IllegalArgumentException {
        return this.saveBulk(index, bulkSaveResult, errorBuffer, documents.toArray(new IIndexable[documents.size()]));
    }

    /**
     * {@link #saveBulk(String, BulkSaveResult, StringBuilder, IIndexable[])}
     */
    default BulkSaveResult saveBulk(final String index,
                                    final Collection<? extends IIndexable> documents,
                                    StringBuilder errorBuffer) throws IllegalArgumentException {
        return this.saveBulk(index, errorBuffer, documents.toArray(new IIndexable[documents.size()]));
    }

    /**
     * Retrieve a Document from its id
     *
     * @param index   index
     * @param docType document type
     * @param docId   document id
     * @param clazz   class of document type
     * @param <T>     document type
     * @return found document or null
     */
    <T extends IIndexable> T get(Optional<String> index, String docType, String docId, Class<T> clazz);

    default <T extends IIndexable> T get(String docType, String docId, Class<T> clazz) {
        return get(Optional.empty(), docType, docId, clazz);
    }

    <T extends IIndexable> T getByVirtualId(String docType, String virtualId, Class<? extends IIndexable> clazz);

    /**
     * Utility method to avoid using Class<T> and passing directly id and type
     *
     * @param index    index
     * @param document IIndexable object specifying docId and type
     * @param <T>      document type
     * @return found document of same type as document or null
     */
    @SuppressWarnings("unchecked")
    default <T extends IIndexable> T get(final Optional<String> index, final T document) {
        return (T) get(index, document.getType(), document.getDocId(), document.getClass());
    }

    /**
     * Delete specified document
     *
     * @param index index
     * @param type  document type
     * @param id    document id
     * @return true if document no more exists, false otherwise
     */
    boolean delete(String index, String type, String id);

    /**
     * Delete all documents from index
     *
     * @param index index
     * @return number of documents deleted
     */
    long deleteAll(String index);

    /**
     * Delete all documents from index following criterion
     *
     * @param index     index
     * @param criterion criterion
     * @return number of deleted elements
     */
    long deleteByQuery(String index, ICriterion criterion);

    /**
     * Same as {@link #delete(String, String, String)} using docId and type of provided document
     *
     * @param index    index
     * @param document IIndexable object specifying docId and type
     * @return true if document no more exists, false otherwise
     */
    default boolean delete(final String index, final IIndexable document) {
        return delete(index, document.getType(), document.getDocId());
    }

    /**
     * Searching first page of elements from index giving page size with facets.
     *
     * @param searchKey  the search key specifying on which index and type the search must be applied and the class of return objects type
     * @param pageSize   page size
     * @param crit       search criterion
     * @param facetsMap  map of (attribute name - facet type). Can be null if no facet asked for.
     * @param ascSortMap map of (attributes name - true if ascending). Can be null if no sort asked for.
     * @param <T>        document type
     * @return first result page containing max page size documents
     */
    default <T extends IIndexable> Page<T> search(final SearchKey<T, T> searchKey,
                                                  final int pageSize,
                                                  final ICriterion crit,
                                                  final Map<String, FacetType> facetsMap,
                                                  final LinkedHashMap<String, Boolean> ascSortMap) {
        return this.search(searchKey,
                           PageRequest.of(0, pageSize, new LinkedHashMapToSort().convert(ascSortMap)),
                           crit,
                           facetsMap);
    }

    /**
     * Searching specified page of elements from index with facets. <b>This method fails if asked for offset greater than
     * 10000 (Elasticsearch limitation)</b>
     *
     * @param pageRequest page request (use {@link Page#nextPageable()} method for example)
     * @param crit        search criterion
     * @param facetsMap   map of (attribute name - facet type). Can be null if no facet asked for.
     * @param searchKey   the search key
     * @param <T>         class of document type
     * @return specified result page
     */
    <T extends IIndexable> FacetPage<T> search(SearchKey<T, T> searchKey,
                                               Pageable pageRequest,
                                               ICriterion crit,
                                               Map<String, FacetType> facetsMap);

    /**
     * Searching first page of elements from index giving page size without facets.
     *
     * @param searchKey the search key specifying on which index and type the search must be applied and the class of return objects type
     * @param pageSize  page size
     * @param crit      search criterion
     * @param <T>       document type
     * @return first result page containing max page size documents
     */
    default <T extends IIndexable> Page<T> search(final SearchKey<T, T> searchKey,
                                                  final int pageSize,
                                                  final LinkedHashMap<String, Boolean> ascSortMap,
                                                  final ICriterion crit) {
        return this.search(searchKey, pageSize, crit, (Map<String, FacetType>) null, ascSortMap);
    }

    /**
     * Searching first page of elements from index giving page size without sort.
     *
     * @param searchKey the search key specifying on which index and type the search must be applied and the class of return objects type
     * @param pageSize  page size
     * @param crit      search criterion
     * @param <T>       document type
     * @return first result page containing max page size documents
     */
    default <T extends IIndexable> Page<T> search(final SearchKey<T, T> searchKey,
                                                  final int pageSize,
                                                  final ICriterion crit,
                                                  final Map<String, FacetType> facetsMap) {
        return this.search(searchKey, pageSize, crit, facetsMap, (LinkedHashMap<String, Boolean>) null);
    }

    /**
     * Searching first page of elements from index giving page size without facets nor sort
     *
     * @param searchKey the search key specifying on which index and type the search must be applied and the class of return objects type
     * @param pageSize  page size
     * @param crit      search criterion
     * @param <T>       document type
     * @return first result page containing max page size documents
     */
    default <T extends IIndexable> Page<T> search(final SearchKey<T, T> searchKey,
                                                  final int pageSize,
                                                  final ICriterion crit) {
        return this.search(searchKey, pageSize, crit, (Map<String, FacetType>) null);
    }

    /**
     * Searching specified page of elements from index without facets nor sort. <b>This method fails if asked for offset
     * greater than 10000 (Elasticsearch limitation)</b>
     *
     * @param searchKey   the search key specifying on which index and type the search must be applied and the class of return objects type
     * @param pageRequest page request (use {@link Page#nextPageable()} method for example)
     * @param crit        search criterion
     * @param <T>         class of document type
     * @return specified result page
     */
    default <T extends IIndexable> Page<T> search(final SearchKey<T, T> searchKey,
                                                  final Pageable pageRequest,
                                                  final ICriterion crit) {
        return this.search(searchKey, pageRequest, crit, (Map<String, FacetType>) null);
    }

    /**
     * Searching first page of elements from index giving page size. The results are reduced to given inner property that's why no sorting can be done.
     *
     * @param searchKey       the search key specifying on which index and type the search must be applied and the class of return objects type
     * @param pageSize        page size
     * @param crit            search criterion
     * @param sourceAttribute if the search is on a document but the result shoult be an inner property of the results documents
     * @param <T>             inner result property type
     * @return first result page containing max page size documents
     */
    default <T extends IIndexable> Page<T> search(final SearchKey<?, T> searchKey,
                                                  final int pageSize,
                                                  final ICriterion crit,
                                                  final String sourceAttribute) {
        return this.search(searchKey, pageSize, crit, sourceAttribute);
    }

    /**
     * Used only for testing purposes
     * Search objects in cache based on criterion
     *
     * @return all results (ordered is garanteed to be always the same)
     */
    <T> List<T> search(SearchKey<?, T> searchKey, ICriterion crit, String pSourceAttribute);

    /**
     * Search objects with criteria retrieved from other objects.
     * First, input objects are retrieved from the cache, then objects requested with matching criteria are retrieved
     * from the ES repository.
     */
    <R, T extends IIndexable> FacetPage<T> search(SearchKey<?, R[]> sourceSearchKey,
                                                  ICriterion sourceSearchCriterion,
                                                  String sourceAttribute,
                                                  Predicate<R> sourceFilterPredicate,
                                                  Function<Set<R>, Page<T>> toAskEntityFct,
                                                  Map<String, FacetType> facetsMap,
                                                  Pageable pageable);

    /**
     * Count result
     *
     * @param searchKey the search key
     * @param criterion search criterion
     */
    <T extends IIndexable> Long count(SearchKey<?, T> searchKey, ICriterion criterion);

    /**
     * Retrieve sum of given attribute
     *
     * @param searchKey the search key
     * @param crit      search criterion
     * @param attName   complete attribute path
     * @return the sum
     */
    <T extends IIndexable> double sum(SearchKey<?, T> searchKey, ICriterion crit, String attName);

    /**
     * Retrieve minimum date of given date attribute
     *
     * @param searchKey the search key
     * @param crit      search criterion
     * @param attName   complete attribute path
     * @return the min
     */
    <T extends IIndexable> OffsetDateTime minDate(SearchKey<?, T> searchKey, ICriterion crit, String attName);

    /**
     * Retrieve maximum date of given date attribute
     *
     * @param searchKey the search key
     * @param crit      search criterion
     * @param attName   complete attribute path
     * @return the max
     */
    <T extends IIndexable> OffsetDateTime maxDate(SearchKey<?, T> searchKey, ICriterion crit, String attName);

    /**
     * Retrieve the desired specific aggregations.
     *
     * @param searchKey the search key
     * @param criterion the search criterion
     * @param aggs      the aggregations wished for
     * @param limit     limit number of aggreation terms to return
     * @return the aggregations
     */
    <T extends IIndexable> Aggregations getAggregationsFor(SearchKey<?, T> searchKey,
                                                           ICriterion criterion,
                                                           Collection<AggregationBuilder> aggs,
                                                           int limit);

    /**
     * Retrieve the desired specific aggregations.
     * @param searchRequest aggregation search context as a record
     * @return the aggregations
     */
    <T extends IIndexable> Aggregations getAggregationsFor(AggregationSearchContext<T> searchRequest);

    /**
     * Retrieve the desired specific aggregations. This method is used to retrieve aggregations using parallel search
     * requests.
     * @param searchRequests multiple aggregation search contexts as a map
     * @return all corresponding aggregations as a map
     */
    <T extends IIndexable> Map<String, AggregationSearchContextResponse> getMultiAggregationsFor(Map<String, AggregationSearchContext<T>> searchRequests);

    /**
     * Retrieve stats for each given attribute
     *
     * @param searchKey  the search key
     * @param crit       search criterion
     * @param attributes non text attributes
     * @return the stats of each attribute
     */
    <T extends IIndexable> Aggregations getAggregations(SearchKey<?, T> searchKey,
                                                        ICriterion crit,
                                                        Collection<QueryableAttribute> attributes);

    /**
     * Retrieve unique sorted string attribute values following given request
     *
     * @param searchKey the search key
     * @param crit      search criterion
     * @param attName   complete string attribute path
     * @param maxCount  maximum count of values
     * @return a soprted set of values
     */
    <T extends IIndexable> SortedSet<String> uniqueAlphaSorted(SearchKey<?, T> searchKey,
                                                               ICriterion crit,
                                                               String attName,
                                                               int maxCount);

    /**
     * Searching first page of elements from index giving page size
     *
     * @param searchKey the search key
     * @param pageSize  page size
     * @param value     value to search
     * @param fields    fields to search on (use '.' for inner objects, ie "attributes.tags"). <b>Fields types must be consistent with given value type</b>
     * @param <T>       document type
     * @return first result page containing max page size documents
     */
    default <T extends IIndexable> Page<T> multiFieldsSearch(final SearchKey<T, T> searchKey,
                                                             final int pageSize,
                                                             final Object value,
                                                             final String... fields) {
        return this.multiFieldsSearch(searchKey, PageRequest.of(0, pageSize), value, fields);
    }

    /**
     * Searching specified page of elements from index giving page size (for first call us {@link #multiFieldsSearch(SearchKey, int, Object, String...)} method
     *
     * @param searchKey   the search key
     * @param pageRequest page request (use {@link Page#nextPageable()} method for example)
     * @param value       value to search
     * @param fields      fields to search on (use '.' for inner objects, ie "attributes.tags"). Wildcards '*' can be used too
     *                    (ie attributes.dataRange.*). <b>Fields types must be consistent with given value type</b>
     * @param <T>         document type
     * @return specified result page
     */
    <T extends IIndexable> Page<T> multiFieldsSearch(SearchKey<T, T> searchKey,
                                                     Pageable pageRequest,
                                                     Object value,
                                                     String... fields);

    /**
     * Execute specified action for all search results<br/>
     * <b>No 10000 offset Elasticsearch limitation</b>
     *
     * @param searchKey the search key specifying the index and type to search and the result class used
     * @param pAction   action to be executed for each search result element
     * @param crit      search criterion
     */
    <T extends IIndexable> void searchAll(SearchKey<T, T> searchKey, Consumer<T> pAction, ICriterion crit);

    /**
     * Fill DocFilesSummary for given request distributing results based on discriminantProperty for given file
     * types. Only internal data files with a strictly positive size are taken into account. This size is used to count
     * files and to compute sum.
     *
     * @param discriminantProperty property used to distribute computed sub-summaries (usually "tags")
     * @param fileTypes            file types concerned by the computation (usually RAWDATA, QUICKLOOK_(HD|MD|SD))
     * @param <T>                  document type (must be of type IIndexable to be searched and IDocFiles to provide "files" property)
     * @see DocFilesSummary
     */
    <T extends IIndexable & IDocFiles> void computeInternalDataFilesSummary(SearchKey<T, T> searchKey,
                                                                            ICriterion crit,
                                                                            String discriminantProperty,
                                                                            Optional<String> discriminentPropertyInclude,
                                                                            DocFilesSummary summary,
                                                                            String... fileTypes);

    /**
     * Fill DocFilesSummary for given request distributing results based on discriminantProperty for given file
     * types. Only external data files with an http or https uri are taken into account. This uri is used to count
     * files. No sum is computed.
     *
     * @param discriminantProperty property used to distribute computed sub-summaries (usually "tags")
     * @param fileTypes            file types concerned by the computation (usually RAWDATA, QUICKLOOK_(HD|MD|SD))
     * @param <T>                  document type (must be of type IIndexable to be searched and IDocFiles to provide "files" property)
     * @see DocFilesSummary
     */
    <T extends IIndexable & IDocFiles> void computeExternalDataFilesSummary(SearchKey<T, T> searchKey,
                                                                            ICriterion crit,
                                                                            String discriminantProperty,
                                                                            Optional<String> discriminentPropertyInclude,
                                                                            DocFilesSummary summary,
                                                                            String... fileTypes);

    /**
     * Close Client
     */
    void close();

    /**
     *
     */
    long deleteByDatasource(String tenant, Long datasourceId);
}
