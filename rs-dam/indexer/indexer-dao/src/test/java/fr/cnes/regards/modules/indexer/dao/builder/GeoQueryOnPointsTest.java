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
package fr.cnes.regards.modules.indexer.dao.builder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.jsoniter.IIndexableJsoniterConfig;
import fr.cnes.regards.framework.multitenant.test.SingleRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.spring.SpringContext;
import fr.cnes.regards.modules.indexer.dao.EsRepository;
import fr.cnes.regards.modules.indexer.dao.deser.JsoniterDeserializeIIndexableStrategy;
import fr.cnes.regards.modules.indexer.dao.mapping.utils.AttrDescToJsonMapping;
import fr.cnes.regards.modules.indexer.dao.spatial.AbstractOnPointsTest;
import fr.cnes.regards.modules.indexer.dao.spatial.GeoHelper;
import fr.cnes.regards.modules.indexer.dao.spatial.ProjectGeoSettings;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.CircleCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.PolygonCriterion;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@TestPropertySource("classpath:test.properties")
@ActiveProfiles("test")
public class GeoQueryOnPointsTest extends AbstractOnPointsTest {

    private static final String INDEX = "test_geo";

    private static Gson gson;

    private static EsRepository repository;

    private static SimpleSearchKey<PointItem> searchKey;

    private static class ItemAdapterFactory extends PolymorphicTypeAdapterFactory<IIndexable> {

        protected ItemAdapterFactory() {
            super(IIndexable.class, "type");
            registerSubtype(PointItem.class, TYPE);
        }
    }

    private static boolean INIT_DONE = false;

    @Value("${regards.elasticsearch.host}")
    private String elasticHost;

    @Value("${regards.elasticsearch.http.port}")
    private int elasticPort;

    @Value("${regards.elasticsearch.http.protocol:http}")
    private String elasticProtocol;

    @Before
    public void setUp() throws Exception {
        // Permit to setUp() as it is static but with the benefit of all Spring annotations (like @Value)
        if (!INIT_DONE) {
            boolean repositoryOK = true;
            try {
                gson = new GsonBuilder().registerTypeAdapterFactory(new ItemAdapterFactory())
                                        .registerTypeAdapter(OffsetDateTime.class,
                                                             new OffsetDateTimeAdapter().nullSafe())
                                        .create();
                repository = new EsRepository(gson,
                                              new JsoniterDeserializeIIndexableStrategy(new IIndexableJsoniterConfig()),
                                              new AggregationBuilderFacetTypeVisitor(100, 5),
                                              new AttrDescToJsonMapping(AttrDescToJsonMapping.RangeAliasStrategy.GTELTE),
                                              new SingleRuntimeTenantResolver("test"),
                                              Collections.emptyList(),
                                              elasticHost,
                                              elasticPort,
                                              elasticProtocol,
                                              null,
                                              null,
                                              0,
                                              15000,
                                              1200000);
            } catch (NoNodeAvailableException e) {
                repositoryOK = false;
            }
            // Do not launch tests if Elasticsearch is not available
            Assume.assumeTrue(repositoryOK);

            GsonUtil.setGson(gson);

            if (repository.indexExists(INDEX)) {
                repository.deleteIndex(INDEX);
            }
            repository.createIndex(INDEX);

            PointItem northPole = new PointItem("NORTH_POLE", 0.0, 90.0);
            PointItem southPole = new PointItem("SOUTH_POLE", 0.0, -90.0);
            PointItem eastPole = new PointItem("EAST_POLE", 180.0, 0.0);
            PointItem westPole = new PointItem("WEST_POLE", -180.0, 0.0);
            PointItem point_180_20 = new PointItem("P1", 180.0, 20.0);
            PointItem point_90_20 = new PointItem("P2", 90.0, 20.0);
            PointItem point_0_0 = new PointItem("0_0", 0.0, 0.0);

            repository.saveBulk(INDEX, northPole, southPole, point_180_20, point_90_20, eastPole, westPole, point_0_0);
            repository.refresh(INDEX);

            searchKey = new SimpleSearchKey<PointItem>(TYPE, PointItem.class);
            searchKey.setSearchIndex(INDEX);

            SpringContext springContext = SpringContext.class.getDeclaredConstructor().newInstance();
            ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
            // Activate "pole geometries management"
            ProjectGeoSettings settings = new ProjectGeoSettings() {

                @Override
                public Boolean getShouldManagePolesOnGeometries() {
                    return true;
                }
            };
            Mockito.when(appContext.getBean(ArgumentMatchers.eq(ProjectGeoSettings.class))).thenReturn(settings);
            springContext.setApplicationContext(appContext);
            INIT_DONE = true;
        }
    }

    @Test
    public void testSimplePolygonQuery() {
        PolygonCriterion criterion = (PolygonCriterion) ICriterion.intersectsPolygon(simplePolygon(0,
                                                                                                   0,
                                                                                                   45,
                                                                                                   0,
                                                                                                   45,
                                                                                                   45));
        QueryBuilderCriterionVisitor visitor = new QueryBuilderCriterionVisitor();
        QueryBuilder builder = visitor.visitPolygonCriterion(criterion);
        Assert.assertEquals("{\n"
                            + "  \"geo_shape\" : {\n"
                            + "    \"wgs84\" : {\n"
                            + "      \"shape\" : {\n"
                            + "        \"type\" : \"Polygon\",\n"
                            + "        \"coordinates\" : [\n"
                            + "          [\n"
                            + "            [\n"
                            + "              0.0,\n"
                            + "              0.0\n"
                            + "            ],\n"
                            + "            [\n"
                            + "              45.0,\n"
                            + "              0.0\n"
                            + "            ],\n"
                            + "            [\n"
                            + "              45.0,\n"
                            + "              45.0\n"
                            + "            ],\n"
                            + "            [\n"
                            + "              0.0,\n"
                            + "              0.0\n"
                            + "            ]\n"
                            + "          ]\n"
                            + "        ]\n"
                            + "      },\n"
                            + "      \"relation\" : \"intersects\"\n"
                            + "    },\n"
                            + "    \"ignore_unmapped\" : false,\n"
                            + "    \"boost\" : 1.0\n"
                            + "  }\n"
                            + "}", builder.toString());
    }

    private double[] point(double... lonLats) {
        assert (lonLats.length == 2);
        return new double[] { lonLats[0], lonLats[1] };
    }

    private double[][][] simplePolygon(Integer... lonLats) {
        assert (lonLats.length >= 6);
        assert ((lonLats.length % 2) == 0);
        double[][] shell = new double[(lonLats.length / 2) + 1][];
        for (int i = 0; i < lonLats.length; i += 2) {
            shell[i / 2] = new double[] { lonLats[i].doubleValue(), lonLats[i + 1].doubleValue() };
        }
        shell[shell.length - 1] = new double[] { lonLats[0].doubleValue(), lonLats[1].doubleValue() };
        return new double[][][] { shell };
    }

    @Test
    public void testPolygonAroundNorthPoleQuery() {
        PolygonCriterion criterion = (PolygonCriterion) ICriterion.intersectsPolygon(simplePolygon(0,
                                                                                                   80,
                                                                                                   90,
                                                                                                   80,
                                                                                                   180,
                                                                                                   80,
                                                                                                   -90,
                                                                                                   80));
        QueryBuilderCriterionVisitor visitor = new QueryBuilderCriterionVisitor();
        QueryBuilder builder = visitor.visitPolygonCriterion(criterion);
        System.out.println(builder.toString());
    }

    @Test
    public void testPolygonOnDateMeridianQuery1() {
        PolygonCriterion criterion = (PolygonCriterion) ICriterion.intersectsPolygon(simplePolygon(170,
                                                                                                   20,
                                                                                                   -170,
                                                                                                   20,
                                                                                                   -170,
                                                                                                   60,
                                                                                                   170,
                                                                                                   60));
        QueryBuilderCriterionVisitor visitor = new QueryBuilderCriterionVisitor();
        QueryBuilder builder = visitor.visitPolygonCriterion(criterion);
        Assert.assertEquals("{\n"
                            + "  \"geo_shape\" : {\n"
                            + "    \"wgs84\" : {\n"
                            + "      \"shape\" : {\n"
                            + "        \"type\" : \"Polygon\",\n"
                            + "        \"coordinates\" : [\n"
                            + "          [\n"
                            + "            [\n"
                            + "              170.0,\n"
                            + "              20.0\n"
                            + "            ],\n"
                            + "            [\n"
                            + "              -170.0,\n"
                            + "              20.0\n"
                            + "            ],\n"
                            + "            [\n"
                            + "              -170.0,\n"
                            + "              60.0\n"
                            + "            ],\n"
                            + "            [\n"
                            + "              170.0,\n"
                            + "              60.0\n"
                            + "            ],\n"
                            + "            [\n"
                            + "              170.0,\n"
                            + "              20.0\n"
                            + "            ]\n"
                            + "          ]\n"
                            + "        ]\n"
                            + "      },\n"
                            + "      \"relation\" : \"intersects\"\n"
                            + "    },\n"
                            + "    \"ignore_unmapped\" : false,\n"
                            + "    \"boost\" : 1.0\n"
                            + "  }\n"
                            + "}", builder.toString());
    }

    @Test
    public void testPolygonOnDateMeridian() {
        PolygonCriterion criterion = (PolygonCriterion) ICriterion.intersectsPolygon(simplePolygon(170,
                                                                                                   19,
                                                                                                   -170,
                                                                                                   19,
                                                                                                   -170,
                                                                                                   60,
                                                                                                   170,
                                                                                                   60));

        List<PointItem> result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("P1", result.get(0).getDocId());
    }

    @Test

    public void testPolygonOnDateMeridianQuery2() {
        PolygonCriterion criterion = (PolygonCriterion) ICriterion.intersectsPolygon(simplePolygon(170,
                                                                                                   60,
                                                                                                   170,
                                                                                                   -20,
                                                                                                   -170,
                                                                                                   -20,
                                                                                                   -170,
                                                                                                   60));

        List<PointItem> result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(3, result.size());
        Assert.assertTrue(result.stream()
                                .map(r -> r.getDocId())
                                .collect(Collectors.toList())
                                .containsAll(Arrays.asList("EAST_POLE", "WEST_POLE", "P1")));
    }

    @Test
    public void testPolygonBetweenTwoMeridiansQuery() {
        PolygonCriterion criterion = (PolygonCriterion) ICriterion.intersectsPolygon(simplePolygon(0,
                                                                                                   90,
                                                                                                   170,
                                                                                                   0,
                                                                                                   0,
                                                                                                   -90,
                                                                                                   -170,
                                                                                                   0));
        //                    .intersectsPolygon(simplePolygon(170, 80, 170, 0, 170, -80, -170, -80, -170, 0, -170, 80));

        QueryBuilderCriterionVisitor visitor = new QueryBuilderCriterionVisitor();
        QueryBuilder builder = visitor.visitPolygonCriterion(criterion);
        System.out.println(builder.toString());
    }

    @Test
    public void testCircleOnNorthPole() {
        CircleCriterion criterion = (CircleCriterion) ICriterion.intersectsCircle(point(0, 90), "10m");

        List<PointItem> result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("NORTH_POLE", result.get(0).getDocId());

        criterion = (CircleCriterion) ICriterion.intersectsCircle(point(0, 90), "15000km");

        result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(6, result.size());
        Assert.assertTrue(result.stream().map(r -> r.getDocId()).collect(Collectors.toList()).contains("NORTH_POLE"));

        criterion = (CircleCriterion) ICriterion.intersectsCircle(point(0, 90), "20050km");

        result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(7, result.size());
        Assert.assertTrue(result.stream().map(r -> r.getDocId()).collect(Collectors.toList()).contains("NORTH_POLE"));
    }

    @Test
    public void testCircleNearNorthPole() {
        CircleCriterion criterion = (CircleCriterion) ICriterion.intersectsCircle(point(60, 89), "112km");

        List<PointItem> result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("NORTH_POLE", result.get(0).getDocId());

        criterion = (CircleCriterion) ICriterion.intersectsCircle(point(60, 89), "110km");

        result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testCircleOnSouthPole() {
        CircleCriterion criterion = (CircleCriterion) ICriterion.intersectsCircle(point(0, -90), "10m");

        List<PointItem> result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("SOUTH_POLE", result.get(0).getDocId());

        criterion = (CircleCriterion) ICriterion.intersectsCircle(point(0, -90), "15000km");

        result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(6, result.size());
        Assert.assertTrue(result.stream().map(r -> r.getDocId()).collect(Collectors.toList()).contains("SOUTH_POLE"));

        criterion = (CircleCriterion) ICriterion.intersectsCircle(point(0, -90), "20050km");

        result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(7, result.size());
        Assert.assertTrue(result.stream()
                                .map(r -> r.getDocId())
                                .collect(Collectors.toList())
                                .containsAll(Arrays.asList("NORTH_POLE", "SOUTH_POLE")));
    }

    @Test
    public void testCircleNearSouthPole() {
        CircleCriterion criterion = (CircleCriterion) ICriterion.intersectsCircle(point(60, -89), "112km");

        List<PointItem> result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("SOUTH_POLE", result.get(0).getDocId());

        criterion = (CircleCriterion) ICriterion.intersectsCircle(point(60, -89), "110km");

        result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testCircleOnEastPole() {
        CircleCriterion criterion = (CircleCriterion) ICriterion.intersectsCircle(point(180, 00), "10m");

        List<PointItem> result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.stream()
                                .map(r -> r.getDocId())
                                .collect(Collectors.toList())
                                .containsAll(Arrays.asList("WEST_POLE", "EAST_POLE")));
    }

    @Test
    public void testCircleOnWestPole() {
        CircleCriterion criterion = (CircleCriterion) ICriterion.intersectsCircle(point(-180, 0), "10m");

        List<PointItem> result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.stream()
                                .map(r -> r.getDocId())
                                .collect(Collectors.toList())
                                .containsAll(Arrays.asList("WEST_POLE", "EAST_POLE")));

        criterion = (CircleCriterion) ICriterion.intersectsCircle(point(-180, 0), "2229.85km");

        result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(3, result.size());
        Assert.assertTrue(result.stream()
                                .map(r -> r.getDocId())
                                .collect(Collectors.toList())
                                .containsAll(Arrays.asList("WEST_POLE", "EAST_POLE", "P1")));

        Integer d = 2211_170;
        criterion = (CircleCriterion) ICriterion.intersectsCircle(point(-180, 0), d.toString());

        System.out.printf("Error : %d m\n", (int) (GeoHelper.getDistanceOnEarth(point(-180, 0), point(180, 20)) - d));

        result = repository.search(searchKey, 1000, criterion).getContent();
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.stream()
                                .map(r -> r.getDocId())
                                .collect(Collectors.toList())
                                .containsAll(Arrays.asList("WEST_POLE", "EAST_POLE")));
    }

    @Test
    public void testPrecisionFor1degree() {
        System.out.println(GeoHelper.getDistanceOnEarth(point(180, 20), point(180, 21)));
        System.out.println(GeoHelper.getDistanceOnEarth(point(0, 89), point(0, 90)));
        System.out.println(GeoHelper.getDistanceOnEarth(point(180, 20), point(180.0, 20.1)));

        Integer d1 = 111_196;
        CircleCriterion criterion = criterion = (CircleCriterion) ICriterion.intersectsCircle(point(180, 21),
                                                                                              d1.toString());

        List<PointItem> result = repository.search(searchKey, 1000, criterion).getContent();

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("P1", result.get(0).getDocId());
        System.out.printf("Error near equator : %d m (radius : %d m)\n",
                          (int) GeoHelper.getDistanceOnEarth(point(180, 20), point(180, 21)) - d1,
                          d1);

        Integer d2 = 111_694;
        criterion = criterion = (CircleCriterion) ICriterion.intersectsCircle(point(0, 89), d2.toString());

        result = repository.search(searchKey, 1000, criterion).getContent();

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("NORTH_POLE", result.get(0).getDocId());
        System.out.printf("Error near pole : %d m (radius : %d m)\n",
                          (int) GeoHelper.getDistanceOnEarth(point(0, 89), point(0, 90)) - d2,
                          d2);

        Integer d3 = 11_120;
        criterion = criterion = (CircleCriterion) ICriterion.intersectsCircle(point(180.0, 20.1), d3.toString());

        result = repository.search(searchKey, 1000, criterion).getContent();

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("P1", result.get(0).getDocId());
        System.out.printf("Error near equator : %d m (radius : %d m)\n",
                          (int) GeoHelper.getDistanceOnEarth(point(180.0, 20.1), point(180, 20)) - d3,
                          d3);

    }

}
