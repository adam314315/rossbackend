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
package fr.cnes.regards.modules.crawler.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.IngestionResult;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.dao.builder.QueryBuilderCriterionVisitor;
import fr.cnes.regards.modules.indexer.domain.JoinEntitySearchKey;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.ParsedMin;
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class IndexerServiceDataSourceMultisearchIT extends AbstractIndexerServiceDataSourceIT {

    @Requirement("REGARDS_DSL_DAM_COL_420")
    @Purpose("Requirement is for collection. Multi search field is used here on data objects but the code is the same")
    @Test
    @Ignore("Bug on elastic 7.16 during computation of aggregation ranges for integer values (ES issue #83212)")
    public void test() throws Exception {
        String tenant = runtimeTenantResolver.getTenant();

        // Creation
        long start = System.currentTimeMillis();
        DatasourceIngestion dsi = new DatasourceIngestion(dataSourcePluginConf.getBusinessId());
        dsi.setLabel("Label");
        dsIngestionRepos.save(dsi);

        IngestionResult summary1 = crawlerService.ingest(dsi.getId()).get();
        System.out.println("Insertion : " + (System.currentTimeMillis() - start) + " ms");

        // Update
        start = System.currentTimeMillis();
        IngestionResult summary2 = crawlerService.ingest(dsi.getId()).get();
        System.out.println("Update : " + (System.currentTimeMillis() - start) + " ms");
        Assert.assertEquals(summary1.getSavedObjectsCount(), summary2.getSavedObjectsCount());

        crawlerService.startWork();
        // Create 3 Datasets on all objects
        dataset1 = new Dataset(datasetModel, tenant, "DS1", "dataset label 1");
        dataset1.setDataModel(dataModel.getName());
        dataset1.setSubsettingClause(ICriterion.all());
        dataset1.setOpenSearchSubsettingClause("");
        dataset1.setLicence("licence");
        dataset1.setPlgConfDataSource(dataSourcePluginConf);
        dataset1.setTags(Sets.newHashSet("BULLSHIT"));
        dataset1.setGroups(Sets.newHashSet("group0", "group11"));
        dsService.create(dataset1);

        dataset2 = new Dataset(datasetModel, tenant, "DS2", "dataset label 2");
        dataset2.setDataModel(dataModel.getName());
        dataset2.setSubsettingClause(ICriterion.all());
        dataset2.setOpenSearchSubsettingClause("");
        dataset2.setTags(Sets.newHashSet("BULLSHIT"));
        dataset2.setLicence("licence");
        dataset2.setPlgConfDataSource(dataSourcePluginConf);
        dataset2.setGroups(Sets.newHashSet("group12", "group11"));
        dsService.create(dataset2);

        dataset3 = new Dataset(datasetModel, tenant, "DS3", "dataset label 3");
        dataset3.setDataModel(dataModel.getName());
        dataset3.setSubsettingClause(ICriterion.all());
        dataset3.setOpenSearchSubsettingClause("");
        dataset3.setLicence("licence");
        dataset3.setPlgConfDataSource(dataSourcePluginConf);
        dataset3.setGroups(Sets.newHashSet("group2"));
        dsService.create(dataset3);

        crawlerService.waitForEndOfWork();
        waitComputation(dataset1.getIpId(), "vcount", 30, TimeUnit.SECONDS, tenant);
        waitComputation(dataset2.getIpId(), "vcount", 30, TimeUnit.SECONDS, tenant);
        waitComputation(dataset3.getIpId(), "vcount", 30, TimeUnit.SECONDS, tenant);

        // Retrieve dataset1 from ES
        dataset1 = searchService.get(dataset1.getIpId());
        Assert.assertNotNull(dataset1);

        // SearchKey<DataObject> objectSearchKey = new SearchKey<>(tenant, EntityType.DATA.toString(),
        // DataObject.class);
        SimpleSearchKey<DataObject> objectSearchKey = Searches.onSingleEntity(EntityType.DATA);
        objectSearchKey.setSearchIndex(tenant);
        // check that computed attribute were correclty done
        checkDatasetComputedAttribute(dataset1, objectSearchKey, summary1.getSavedObjectsCount());
        // Search for DataObjects tagging dataset1
        Page<DataObject> objectsPage = searchService.search(objectSearchKey,
                                                            10000,
                                                            ICriterion.eq("tags",
                                                                          dataset1.getIpId().toString(),
                                                                          StringMatchType.KEYWORD));
        Assert.assertTrue(objectsPage.getContent().size() > 0);
        Assert.assertEquals(summary1.getSavedObjectsCount(), objectsPage.getContent().size());
        IProperty<JsonObject> rawFeature = (IProperty<JsonObject>) objectsPage.getContent()
                                                                              .get(0)
                                                                              .getFeature()
                                                                              .getProperties()
                                                                              .stream()
                                                                              .filter(p -> p.getName()
                                                                                            .equals("raw_feature"))
                                                                              .findFirst()
                                                                              .get();
        Assert.assertNotNull(rawFeature);
        Assert.assertEquals("Expted value for property in JsonObject attribute is not found",
                            "ici",
                            rawFeature.getValue().get("street_address").getAsString());

        crawlerService.startWork();
        // Delete dataset1
        dsService.delete(dataset1.getId());

        // Wait a while to permit RabbitMq sending a message to crawler service which update ES
        crawlerService.waitForEndOfWork();

        // Search again for DataObjects tagging this dataset
        objectsPage = searchService.search(objectSearchKey,
                                           10000,
                                           ICriterion.eq("tags",
                                                         dataset1.getIpId().toString(),
                                                         StringMatchType.KEYWORD));
        Assert.assertTrue(objectsPage.getContent().isEmpty());
        // Adding some free tag
        objectsPage.getContent().forEach(object -> object.addTags("TOTO"));
        esRepos.saveBulk(tenant, objectsPage.getContent());

        esRepos.refresh(tenant);

        // Search for DataObjects tagging dataset2
        objectsPage = searchService.search(objectSearchKey,
                                           10000,
                                           ICriterion.eq("tags",
                                                         dataset2.getIpId().toString(),
                                                         StringMatchType.KEYWORD));
        Assert.assertTrue(objectsPage.getContent().size() > 0);
        Assert.assertEquals(summary1.getSavedObjectsCount(), objectsPage.getContent().size());

        // Search for Dataset but with criterion on DataObjects
        // SearchKey<Dataset> dsSearchKey = new SearchKey<>(tenant, EntityType.DATA.toString(), Dataset.class);
        JoinEntitySearchKey<DataObject, Dataset> dsSearchKey = Searches.onSingleEntityReturningJoinEntity(EntityType.DATA,
                                                                                                          EntityType.DATASET);
        dsSearchKey.setSearchIndex(tenant);
        Map<String, FacetType> facetsMap = new ImmutableMap.Builder<String, FacetType>().put(
                                                                                            "feature.properties.DATASET_VALIDATION_TYPE",
                                                                                            FacetType.STRING)
                                                                                        .put("feature.properties.weight",
                                                                                             FacetType.NUMERIC)
                                                                                        .put("feature.properties.vdate",
                                                                                             FacetType.DATE)
                                                                                        .build();
        FacetPage<Dataset> dsPage = searchService.search(dsSearchKey, 1, ICriterion.all(), facetsMap);
        Assert.assertNotNull(dsPage);
        Assert.assertFalse(dsPage.getContent().isEmpty());
        Assert.assertEquals("There should be 3 facets. Yes we have to compute data facets when looking for datasets",
                            3,
                            dsPage.getFacets().size());
        Assert.assertEquals(1, dsPage.getContent().size());

        dsPage = searchService.search(dsSearchKey, dsPage.nextPageable(), ICriterion.all(), facetsMap);
        Assert.assertNotNull(dsPage);
        Assert.assertFalse(dsPage.getContent().isEmpty());
        Assert.assertEquals("There should be 3 facets", 3, dsPage.getFacets().size());
        Assert.assertEquals(1, dsPage.getContent().size());

        // Search for Dataset but with criterion on everything
        // SearchKey<Dataset> dsSearchKey2 = new SearchKey<>(tenant, EntityType.DATA.toString(), Dataset.class);
        @SuppressWarnings("rawtypes")
        final JoinEntitySearchKey<AbstractEntity, Dataset> dsSearchKey2 = Searches.onAllEntitiesReturningJoinEntity(
            EntityType.DATASET);
        dsSearchKey2.setSearchIndex(tenant);
        dsPage = searchService.search(dsSearchKey, 1, ICriterion.all(), null);
        Assert.assertNotNull(dsPage);
        Assert.assertFalse(dsPage.getContent().isEmpty());
        Assert.assertEquals(1, dsPage.getContent().size());

        dsPage = searchService.search(dsSearchKey2, dsPage.nextPageable(), ICriterion.all(), null);
        Assert.assertNotNull(dsPage);
        Assert.assertFalse(dsPage.getContent().isEmpty());
        Assert.assertEquals(1, dsPage.getContent().size());
    }

    /**
     * Add document type (Elasticsearch prior to version 6 type) into criterion
     * (Copied from EsRepository)
     */
    private static ICriterion addTypes(ICriterion criterion, String... types) {
        // Beware if crit is null
        criterion = criterion == null ? ICriterion.all() : criterion;
        // Then add type
        switch (types.length) {
            case 0:
                return criterion;
            case 1:
                return ICriterion.and(ICriterion.eq("type", types[0], StringMatchType.KEYWORD), criterion);
            default:
                ICriterion orCrit = ICriterion.or(Arrays.stream(types)
                                                        .map(type -> ICriterion.eq("type",
                                                                                   type,
                                                                                   StringMatchType.KEYWORD))
                                                        .toArray(n -> new ICriterion[n]));
                return ICriterion.and(orCrit, criterion);
        }

    }

    private void checkDatasetComputedAttribute(Dataset dataset,
                                               SimpleSearchKey<DataObject> objectSearchKey,
                                               long objectsCreationCount) throws IOException {
        RestClientBuilder restClientBuilder;
        RestHighLevelClient client;
        try {
            restClientBuilder = RestClient.builder(new HttpHost(InetAddress.getByName(esHost), esPort));
            client = new RestHighLevelClient(restClientBuilder);

        } catch (final UnknownHostException e) {
            LOGGER.error("could not get a connection to ES in the middle of the test where we know ES is available", e);
            Assert.fail();
            return;
        }
        // lets build the request so elasticsearch can calculate the few attribute we are using in test(min(START_DATE),
        // max(STOP_DATE), sum(FILE_SIZE) via aggregation, the count of element in this context is already known:
        // objectsCreationCount
        QueryBuilderCriterionVisitor critVisitor = new QueryBuilderCriterionVisitor();
        ICriterion crit = ICriterion.eq("tags", dataset.getIpId().toString(), StringMatchType.KEYWORD);
        crit = addTypes(crit, objectSearchKey.getSearchTypes());
        QueryBuilder qb = QueryBuilders.boolQuery()
                                       .must(QueryBuilders.matchAllQuery())
                                       .filter(crit.accept(critVisitor));
        // now we have a request on the right data
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.query(qb).size(0);
        // lets build the aggregations
        // aggregation for the min
        builder.aggregation(AggregationBuilders.min("min_start_date")
                                               .field(StaticProperties.FEATURE_PROPERTIES_PATH + ".vdate"));
        // aggregation for the max
        builder.aggregation(AggregationBuilders.max("max_stop_date")
                                               .field(StaticProperties.FEATURE_PROPERTIES_PATH + ".vdate"));
        // aggregation for the sum
        builder.aggregation(AggregationBuilders.sum("sum_values_l1")
                                               .field(StaticProperties.FEATURE_PROPERTIES_PATH + ".value_l1"));
        SearchRequest request = new SearchRequest(objectSearchKey.getSearchIndex().toLowerCase()).source(builder);

        // get the results computed by ElasticSearch
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        Map<String, Aggregation> aggregations = response.getAggregations().asMap();

        // now lets actually test things
        Assert.assertEquals(objectsCreationCount, getDatasetProperty(dataset, "vcount").getValue());
        Assert.assertEquals((long) ((ParsedSum) aggregations.get("sum_values_l1")).getValue(),
                            getDatasetProperty(dataset, "values_l1_sum").getValue());
        // lets convert both dates to instant, it is the simpliest way to compare them
        Assert.assertEquals(Instant.parse(((ParsedMin) aggregations.get("min_start_date")).getValueAsString()),
                            ((OffsetDateTime) getDatasetProperty(dataset, "start_date").getValue()).toInstant());
        Assert.assertEquals(Instant.parse(((ParsedMax) aggregations.get("max_stop_date")).getValueAsString()),
                            ((OffsetDateTime) getDatasetProperty(dataset, "end_date").getValue()).toInstant());
        client.close();
    }

    private IProperty<?> getDatasetProperty(final Dataset pDataset, final String pPropertyName) {
        return pDataset.getProperties().stream().filter(p -> p.getName().equals(pPropertyName)).findAny().orElse(null);
    }

}