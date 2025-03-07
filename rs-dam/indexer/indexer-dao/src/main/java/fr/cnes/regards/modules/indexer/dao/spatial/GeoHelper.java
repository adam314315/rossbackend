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
package fr.cnes.regards.modules.indexer.dao.spatial;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Table;
import fr.cnes.regards.framework.geojson.coordinates.PolygonPositions;
import fr.cnes.regards.framework.geojson.coordinates.Position;
import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.geometry.*;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.spring.SpringContext;
import fr.cnes.regards.modules.indexer.dao.spatial.builders.CoordinatesBuilder;
import fr.cnes.regards.modules.indexer.dao.spatial.builders.PolygonBuilder;
import fr.cnes.regards.modules.indexer.domain.criterion.*;
import fr.cnes.regards.modules.indexer.domain.spatial.Crs;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.common.geo.Orientation;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.hipparchus.geometry.partitioning.Region;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.util.FastMath;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

/**
 * Geo spatial utilities class
 *
 * @author oroussel
 */
public final class GeoHelper {

    /**
     * Radius used by ASTRO projection (perfect sphere used, no flattening)
     */
    public static final double AUTHALIC_SPHERE_RADIUS = 6371007.0;

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoHelper.class);

    private static final Table<Crs, Crs, MathTransform> TRANSFORM_TABLE = HashBasedTable.create();

    /**
     * Geodetic calculator map
     */
    private static final EnumMap<Crs, GeodeticCalculator> calcMap = new EnumMap<>(Crs.class);

    private static final EnumMap<Crs, CoordinateReferenceSystem> crsMap = new EnumMap<>(Crs.class);

    /**
     * Geodetic calculator map initializer
     */
    static {
        for (Crs crs : Crs.values()) {
            try {
                CoordinateReferenceSystem coordinateReferenceSystem = CRS.parseWKT(crs.getWkt());
                crsMap.put(crs, coordinateReferenceSystem);
                calcMap.put(crs, new GeodeticCalculator(coordinateReferenceSystem));
            } catch (FactoryException e) {
                LOGGER.error("Bad WKT", e);
            }

        }
        for (Crs crs : Crs.values()) {
            for (Crs otherCrs : Crs.values()) {
                if (crs != otherCrs) {
                    try {
                        MathTransform transform = CRS.findMathTransform(crsMap.get(crs), crsMap.get(otherCrs), true);
                        TRANSFORM_TABLE.put(crs, otherCrs, transform);
                    } catch (FactoryException e) {
                        LOGGER.error("No math transform can be created for given source and destination", e);
                    }
                }
            }
        }
    }

    private static final S2Point NORTH_POLE_AS_S2_POINT = new S2Point(toTheta(0.0), toPhi(90.0));

    private static final S2Point SOUTH_POLE_AS_S2_POINT = new S2Point(toTheta(0.0), toPhi(-90.0));

    /**
     * Max "cheated" longitude. Used when normalizing a polygon which pass through dateline.
     */
    private static final double MAX_CHEATED_LONGITUDE = 359.999999999999;

    private static final GeometryNormalizerVisitor GEOMETRY_NORMALIZER_VISITOR = new GeometryNormalizerVisitor();

    private static final String ERROR_TRANSFORM = "Error while trying to transform ";

    private GeoHelper() {
    }

    public static double getDistanceOnEarth(double[] lonLat1, double[] lonLat2) {
        return getDistance(lonLat1, lonLat2, Crs.WGS_84);
    }

    public static double getDistanceOnEarth(double lon1, double lat1, double lon2, double lat2) {
        return getDistance(lon1, lat1, lon2, lat2, Crs.WGS_84);
    }

    public static double getDistanceOnMars(double[] lonLat1, double[] lonLat2) {
        return getDistance(lonLat1, lonLat2, Crs.MARS_49900);
    }

    public static double getDistanceOnMars(double lon1, double lat1, double lon2, double lat2) {
        return getDistance(lon1, lat1, lon2, lat2, Crs.MARS_49900);
    }

    public static double getDistance(double[] lonLat1, double[] lonLat2, Crs crs) {
        return getDistance(lonLat1[0], lonLat1[1], lonLat2[0], lonLat2[1], crs);
    }

    public static double getDistance(double lon1, double lat1, double lon2, double lat2, Crs crs) {
        GeodeticCalculator calc = calcMap.get(crs);
        calc.setStartingGeographicPoint(lon1, lat1);
        calc.setDestinationGeographicPoint(lon2, lat2);
        return calc.getOrthodromicDistance();
    }

    public static double[] getPointAtDirectionOnEarth(double srcLon, double srcLat, double azimuth, double distance)
        throws TransformException {
        return getPointAtDirection(srcLon, srcLat, azimuth, distance, Crs.WGS_84);
    }

    public static double[] getPointAtDirectionOnEarth(double[] srcLonLat, double azimuth, double distance)
        throws TransformException {
        return getPointAtDirection(srcLonLat, azimuth, distance, Crs.WGS_84);
    }

    public static double[] getPointAtDirectionOnMars(double srcLon, double srcLat, double azimuth, double distance)
        throws TransformException {
        return getPointAtDirection(srcLon, srcLat, azimuth, distance, Crs.MARS_49900);
    }

    public static double[] getPointAtDirectionOnMars(double[] srcLonLat, double azimuth, double distance)
        throws TransformException {
        return getPointAtDirection(srcLonLat, azimuth, distance, Crs.MARS_49900);
    }

    public static double[] getPointAtDirection(double[] srcLonLat, double azimuth, double distance, Crs crs)
        throws TransformException {
        GeodeticCalculator calc = calcMap.get(crs);
        calc.setStartingGeographicPoint(srcLonLat[0], srcLonLat[1]);
        calc.setDirection(azimuth, distance);
        return calc.getDestinationPosition().getCoordinate();
    }

    public static double[] getPointAtDirection(double srcLon, double srcLat, double azimuth, double distance, Crs crs)
        throws TransformException {
        GeodeticCalculator calc = calcMap.get(crs);
        calc.setStartingGeographicPoint(srcLon, srcLat);
        calc.setDirection(azimuth, distance);
        return calc.getDestinationPosition().getCoordinate();
    }

    public static double[] transform(double[] fromPoint, Crs fromCrs, Crs toCrs) {
        MathTransform transform = TRANSFORM_TABLE.get(fromCrs, toCrs);
        if (transform == null) {
            throw new RsRuntimeException(ERROR_TRANSFORM + fromPoint);
        }
        DirectPosition srcPos = new DirectPosition2D(fromPoint[0], fromPoint[1]);
        DirectPosition destPos = null;
        try {
            destPos = transform.transform(srcPos, null);
        } catch (TransformException e) {
            throw new RsRuntimeException(e);
        }
        return destPos.getCoordinate();
    }

    public static double[][] transform(double[][] fromPoints, Crs fromCrs, Crs toCrs) {
        MathTransform transform = TRANSFORM_TABLE.get(fromCrs, toCrs);
        if (transform == null) {
            throw new RsRuntimeException(ERROR_TRANSFORM + fromPoints);
        }
        double[][] toPoints = new double[fromPoints.length][];
        for (int i = 0; i < fromPoints.length; i++) {
            double[] fromPoint = fromPoints[i];
            DirectPosition srcPos = new DirectPosition2D(fromPoint[0], fromPoint[1]);
            DirectPosition destPos = null;
            try {
                destPos = transform.transform(srcPos, null);
            } catch (TransformException e) {
                throw new RsRuntimeException(e);
            }
            toPoints[i] = destPos.getCoordinate();
        }
        return toPoints;
    }

    public static double[][][] transform(double[][][] fromPointsLines, Crs fromCrs, Crs toCrs) {
        MathTransform transform = TRANSFORM_TABLE.get(fromCrs, toCrs);
        if (transform == null) {
            throw new RsRuntimeException(ERROR_TRANSFORM + fromPointsLines);
        }
        double[][][] toPointsLines = new double[fromPointsLines.length][][];
        try {
            for (int i = 0; i < fromPointsLines.length; i++) {
                toPointsLines[i] = new double[fromPointsLines[i].length][];
                for (int j = 0; j < fromPointsLines[i].length; j++) {
                    double[] fromPoint = fromPointsLines[i][j];
                    DirectPosition srcPos = new DirectPosition2D(fromPoint[0], fromPoint[1]);
                    DirectPosition destPos = transform.transform(srcPos, null);
                    toPointsLines[i][j] = destPos.getCoordinate();
                }
            }
        } catch (TransformException e) {
            throw new RsRuntimeException(e);
        }

        return toPointsLines;
    }

    /**
     * Transform geometry from one Crs to another
     */
    public static IGeometry transform(IGeometry geometry, Crs fromCrs, Crs toCrs) {
        GeometryTransformerVisitor visitor = new GeometryTransformerVisitor(fromCrs, toCrs);
        return geometry.accept(visitor);
    }

    /**
     * Does criterion tree contain a CircleCriterion ?
     */
    public static boolean containsCircleCriterion(ICriterion criterion) {
        PredicateCriterionVisitor visitor = new PredicateCriterionVisitor(crit -> crit instanceof CircleCriterion);
        return criterion.accept(visitor);
    }

    /**
     * Find the (first) CircleCriterion from a ICriterion tree
     */
    public static CircleCriterion findCircleCriterion(ICriterion criterion) {
        return criterion.accept(new FinderCriterionVisitor<>(CircleCriterion.class));
    }

    /**
     * Find the (first) PolygonCriterion from a ICriterion tree
     */
    public static PolygonCriterion findPolygonCriterion(ICriterion criterion) {
        return criterion.accept(new FinderCriterionVisitor<>(PolygonCriterion.class));
    }

    /**
     * Does criterion tree contain a PolygonCriterion or a BoundaryBoxCriterion ?
     */
    public static boolean containsPolygonOrBboxCriterion(ICriterion criterion) {
        PredicateCriterionVisitor visitor = new PredicateCriterionVisitor(crit -> (crit instanceof PolygonCriterion)
                                                                                  || (crit instanceof BoundaryBoxCriterion));
        return criterion.accept(visitor);
    }

    /**
     * Determine wether or not given shape is nearer than distance from point into given Crs.
     * As a simplification, 9 more points are added into each polygon segments to be used to determine minimum distance
     * between a point and a segment
     */
    public static boolean isNearer(IGeometry shape, double[] point, double distance, Crs crs) {
        NearestFromDistanceGeometryVisitor visitor = new NearestFromDistanceGeometryVisitor(point, distance, crs);
        return shape.accept(visitor);
    }

    /**
     * Compute distance between given shape and point into given Crs.
     * As a simplification, 9 more points are added into each polygon segments to compute minimum distance between a
     * point and a segment
     */
    public static double getDistance(IGeometry shape, double[] point, Crs crs) {
        DistanceToPointGeometryVisitor visitor = new DistanceToPointGeometryVisitor(point, crs);
        return shape.accept(visitor);
    }

    /**
     * Return a list of ten positions between two points
     */
    private static List<Position> createIntermediatePositions(Position firstPos, Position lastPos) {
        List<Position> positions = new ArrayList<>();
        double latitudeStep = (lastPos.getLatitude() - firstPos.getLatitude()) / 9.0;
        double longitudeStep = (lastPos.getLongitude() - firstPos.getLongitude()) / 9.0;
        positions.add(firstPos);
        for (int n = 1; n < 9; n++) {
            positions.add(new Position(firstPos.getLongitude() + (n * longitudeStep),
                                       firstPos.getLatitude() + (n * latitudeStep)));
        }
        positions.add(lastPos);
        return positions;
    }

    private static double distToDateline(double lon) {
        if (lon > 0) {
            if (lon < 180) {
                return 180 - lon;
            } else {
                return lon - 180;
            }
        } else { // lon < 0
            return -(-180 - lon);
        }
    }

    private static double distTo0(double lon) {
        if (lon > 0) {
            if (lon < 180) {
                return lon;
            } else {
                return 360 - lon;
            }
        } else { // lon < 0
            return -lon;
        }
    }

    public static boolean normalizeNextCoordinate(double[] previous, double[] next) {
        boolean updatePrevious = false;
        // Using only longitude values
        double lonP = previous[0];
        double lonN = next[0];
        LOGGER.trace("IN ({}, {})", lonP, lonN);
        // Previous longitude < 180
        if ((lonP >= 0) && (lonP < 180.0)) {
            // -180 <= next.longitude < 0
            if ((lonN < 0) && (lonN >= -180.0)) {
                // Minimum distance through dateline ?
                if ((distTo0(lonP) + distTo0(lonN)) > (distToDateline(lonP) + distToDateline(lonN))) {
                    // In this case, use longitude > 180 numeric
                    next[0] += 360.0;
                }
            } else if ((lonN > 180) && (lonN < 360.0)) { // 180 <= next.longitude < 360
                // Minimum distance through 0 ?
                if ((distTo0(lonP) + distTo0(lonN)) < (distToDateline(lonP) + distToDateline(lonN))) {
                    // In this case, use longitude < -180 numeric
                    next[0] -= 360.0;
                }
            }
        } else if (lonP >= 180.0) { // Previous longitude >= 180
            // -180 <= next.longitude < 0
            if ((lonN < 0) && (lonN >= -180.0)) {
                // Continue to use longitude > 180 numeric
                next[0] += 360.0;
            } else if (lonN == 0.0) {
                // Mac Gyver hack when latitude is on both poles and previous longitude is >= 180 and next is 0
                // 0 makes Elasticsearch thinking polygon go from 180 to 0 through 90
                // To make it go from 180 to 0 through 270 we stop at 359.9999...(no tool can be as precise as 12
                // decimals)
                next[0] = MAX_CHEATED_LONGITUDE;
            }
        } else { // Previous longitude < 0
            // Next longitude > 180
            if (lonN > 180) {
                // This is unusual, next has certainly been updated because of next case, so update previous accordingly
                // (helpful for recursivity)
                previous[0] += 360.0;
                updatePrevious = true;
            } else if ((lonN >= 0) && (lonN < 180)) { // 0 <= next longitude < 180
                // Minimum distance through dateline ?
                if ((distToDateline(lonP) + distToDateline(lonN)) < (distTo0(lonP) + distTo0(lonN))) {
                    // THIS TIME UPDATE previous !!!!
                    previous[0] += 360.0;
                    updatePrevious = true;
                }
            }
        }
        LOGGER.trace(" -> OUT ({}, {})", previous[0], next[0]);
        return updatePrevious;
    }

    /**
     * Normalize geometry.<br>
     * For polygons:<br/>
     * - update longitudes in order to ease polygon understanding (for Elasticsearch and other geospatial softwares
     * or frameworks) especially when dateLine is crossed. This takes into account the ability to use [-180; 180[ or
     * [0;360[ longitude ranges that are permitted by these softwares or frameworks.<br/>
     * - manage poles by detecting if they are inside polygon (thanks to Hipparchus framework) and append associated
     * "false" rectangle to make it fully functional even with a WGS84 projection (which is the norm for
     * Elasticsearch).<br/>
     * For multi-polygons:<br/>
     * - same thing as for polygon.<br/>
     * For line strings:<br/>
     * - transform lineStrings into MultiLineStrings if they go through date line (check with shortest distance ONLY
     * using longitude, some cases may not be well managed).<br/>
     * For multi line strings:<br/>
     * - apply normalization to all line strings so may add additional line strings if some are transformed into multi
     * line strings.<br/>
     */
    public static IGeometry normalize(IGeometry geometry) {
        return geometry.accept(GEOMETRY_NORMALIZER_VISITOR);
    }

    /**
     * Normalize MultiLineString
     */
    public static MultiLineString normalizeMultiLineString(MultiLineString multiLineString) {
        MultiLineString normMultiLineString = new MultiLineString();
        for (Positions positions : multiLineString.getCoordinates()) {
            IGeometry normGeometry = normalizeLineString(IGeometry.lineString(positions));
            if (normGeometry instanceof LineString) {
                normMultiLineString.getCoordinates().add(((LineString) normGeometry).getCoordinates());
            } else if (normGeometry instanceof MultiLineString) {
                normMultiLineString.getCoordinates().addAll(((MultiLineString) normGeometry).getCoordinates());
            }
        }
        return normMultiLineString;
    }

    /**
     * Normalize lineString
     *
     * @return a LineString or a MultiLineString if it crosses dateLine
     */
    public static IGeometry normalizeLineString(LineString lineString) {
        Positions positions = lineString.getCoordinates();
        // First, normalize position with a longitude between 180 and 360
        for (Position p : positions) {
            if (p.getLongitude() > 180) {
                p.setLongitude(p.getLongitude() - 360);
            }
        }
        List<Positions> positionsList = new ArrayList<>();

        Positions curPositions = new Positions();
        curPositions.add(positions.get(0));
        // For all line string positions
        for (int i = 1; i < positions.size(); i++) {
            Position previous = positions.get(i - 1);
            Position current = positions.get(i);
            double prevLon = previous.getLongitude();
            double curLon = current.getLongitude();
            // sign changing (going through a 0-longitude point may not be a problem)
            if ((prevLon * curLon) < 0) {
                // check if it goes through date line or 0-meridian
                if ((distToDateline(prevLon) + distToDateline(curLon)) < (distTo0(prevLon) + distTo0(curLon))) {
                    // Shortest distance is through date line => cut LineString => MultiLineString
                    // Compute intersection with date line point latitude
                    // Using longitudes > 0
                    double prevLon_360 = prevLon < 0 ? prevLon + 360 : prevLon;
                    double curLon_360 = curLon < 0 ? curLon + 360 : curLon;
                    double prevLat = previous.getLatitude();
                    double curLat = current.getLatitude();
                    double cutLat = (((180 - prevLon_360) * (curLat - prevLat)) / (curLon_360 - prevLon_360)) + prevLat;
                    // Create last position of current positions (ie current line string)
                    curPositions.add(new Position(prevLon < 0 ? -180.0 : 180.0, cutLat));
                    // Add current positions to positions list (=> MultiLineString)
                    positionsList.add(curPositions);
                    // Create a new current positions
                    curPositions = new Positions();
                    // And create first position of new current Positions (ie new line string)
                    curPositions.add(new Position(prevLon < 0 ? 180.0 : -180.0, cutLat));
                }
                curPositions.add(current);
            } else { // Just add current position to current positions
                curPositions.add(current);
            }
        }
        // If positionsList contains at least one positions (ie LineString), this means normalization has transformed
        // input line string into multi line string
        if (!positionsList.isEmpty()) {
            positionsList.add(curPositions);
            MultiLineString multiLineString = new MultiLineString();
            multiLineString.setCoordinates(positionsList);
            return multiLineString;
        }
        return IGeometry.lineString(curPositions);
    }

    /**
     * Normalize polygon without hole
     */
    public static MultiPolygon normalizePolygon(Polygon polygon) {
        // Too complex if polygon contains holes
        if (polygon.containsHoles()) {
            double[][][][] singlePolygon = { polygon.toArray() };
            return MultiPolygon.fromArray(singlePolygon);
        }
        List<double[][][]> polygonList = normalizeExteriorRing(polygon.toArray());
        double[][][][] polygonListAsDouble = new double[polygonList.size()][][][];
        for (int i = 0; i < polygonList.size(); i++) {
            polygonListAsDouble[i] = polygonList.get(i);
        }
        return MultiPolygon.fromArray(polygonListAsDouble);
    }

    /**
     * Normalize multi polygon
     */
    public static MultiPolygon normalizeMultiPolygon(MultiPolygon multiPolygon) {
        List<double[][][]> polygonList = new ArrayList<>();
        for (PolygonPositions positions : multiPolygon.getCoordinates()) {
            // Doesn't manage polygons with holes
            if (positions.getHoles().isEmpty()) {
                List<double[][][]> polygons = normalizeExteriorRing(positions.toArray());
                polygonList.addAll(polygons);
            }
        }
        // Now we just transform List<...> into double[]<...>
        double[][][][] polygonListAsDouble = new double[polygonList.size()][][][];
        for (int i = 0; i < polygonList.size(); i++) {
            polygonListAsDouble[i] = polygonList.get(i);
        }
        return MultiPolygon.fromArray(polygonListAsDouble);
    }

    private static List<double[][][]> normalizeExteriorRing(double[][][] inPolygon) {

        // Let's first found out if uncleaned / original polygon contains poles
        ProjectGeoSettings settings = SpringContext.getBean(ProjectGeoSettings.class);
        boolean northPoleIn = false;
        boolean southPoleIn = false;

        if (settings.getShouldManagePolesOnGeometries()) {
            SphericalPolygonsSet sphericalPolygon = toSphericalPolygonSet(inPolygon[0]);
            // Is North Pole inside polygon
            northPoleIn = sphericalPolygon.checkPoint(NORTH_POLE_AS_S2_POINT) == Region.Location.INSIDE;
            // Is south pole inside polygon ?
            southPoleIn = sphericalPolygon.checkPoint(SOUTH_POLE_AS_S2_POINT) == Region.Location.INSIDE;

            if (northPoleIn && southPoleIn) {
                LOGGER.warn("The same polygon passing threw both poles ?? You should defintly make two polygons !");
            }
        }

        // Let's submit the polygon to the ES java library in order to clean it
        List<double[][][]> normalizedMultiPolygon = sanitizePolygon(inPolygon,
                                                                    settings.getShouldManagePolesOnGeometries(),
                                                                    settings.checkPolygonOrientation());

        // Second normalization phase only if pole management is asked to be done
        if (settings.getShouldManagePolesOnGeometries()) {
            for (int i = 0; i < normalizedMultiPolygon.size(); i++) {
                double[][] exteriorRing = normalizedMultiPolygon.get(i)[0];
                double[][] doubles = cleanPolePolygon(exteriorRing, northPoleIn, southPoleIn);
                normalizedMultiPolygon.set(i, new double[][][] { doubles });
            }
        }

        return normalizedMultiPolygon;
    }

    private static double[][] cleanPolePolygon(double[][] exteriorRing, boolean northPoleIn, boolean southPoleIn) {
        // Second: if polygon is around a pole WITHOUT using it in its exterior ring, a "pass around pole" deviation is
        // added. The idea is to reach north pole (f. example) with (90, x) than adding (90, x + 90), (90, x + 180),
        // (90, x + 270), etc.... and finish with (90, x).
        // Because of the cylindric projection, projected polygon does not reach 90° of latitude (except when it passes
        // through it). For example, a simple 80° latitude polygon "turning" around north pole is represented as a
        // simple line. To add the "pole deviation", it is necessary add a "hat" reaching 90° of latitude on top of the
        // polygon. Knowing max longitude point is connected to a point that is on the left border of the polygon
        // (mostly the min longitude point but not necessarily), we just need to add a point on the max longitude (ie
        // dateline or 0-meridian depending on the max longitude value) and a median latitude between max longitude
        // point and left border one, go straight the north until 90° (with a constant longitude), go "to the west" at
        // minimum longitude (dateline or 0-meridian depending on the case) keeping latitude of 90°, go to the south
        // reaching median latitude then reach the left border point.
        if (!goesThroughNorthPole(exteriorRing)) {
            if (!goesThroughSouthPole(exteriorRing)) {
                // North Pole is inside polygon
                if (northPoleIn) {
                    LOGGER.trace("NORTH POLE is inside polygon");
                    exteriorRing = normalizePolygonAroundNorthPole(exteriorRing);
                }
                // South Pole is inside polygon
                if (southPoleIn) {
                    LOGGER.trace("SOUTH POLE is inside polygon");
                    if (!northPoleIn) {
                        // Only South pole, best way is to apply whole algorithm on ecuadorian symetric polygon
                        return getSymetricPolygon(cleanPolePolygon(getSymetricPolygon(exteriorRing), true, false));
                    } else { // Both poles...ouch, this will be tricky...but who knows...
                        // Work with ecuadorian symetric polygon as if north pole is south pole
                        exteriorRing = getSymetricPolygon(normalizePolygonAroundNorthPole(getSymetricPolygon(
                            exteriorRing)));
                    }
                }
            }
        }
        return exteriorRing;
    }

    private static double[][][] esGeometryToPolygon(Geometry geometry) {
        double[][][] currentPolygon = new double[1][][];
        double[][] coordinates = new double[geometry.getCoordinates().length][];
        int i = 0;
        for (Coordinate coordinate : geometry.getCoordinates()) {
            coordinates[i] = new double[] { coordinate.getX(), coordinate.getY() };
            i++;
        }
        currentPolygon[0] = coordinates;
        return currentPolygon;
    }

    public static String getGeometryAsText(List<double[][][]> multiPolygon) {
        StringJoiner sb = new StringJoiner(",", "[", "]");
        for (int i = 0; i < multiPolygon.size(); i++) {
            StringJoiner polygonSb = new StringJoiner(",", "[", "]");
            for (int j = 0; j < multiPolygon.get(i).length; j++) {
                StringJoiner coordListSb = new StringJoiner(",", "[", "]");
                for (int k = 0; k < multiPolygon.get(i)[j].length; k++) {
                    coordListSb.add("[" + multiPolygon.get(i)[j][k][0] + "," + multiPolygon.get(i)[j][k][1] + "]");
                }
                polygonSb.add(coordListSb.toString());
            }
            sb.add(polygonSb.toString());

        }
        return sb.toString();
    }

    private static List<double[][][]> sanitizeMultiPolygon(double[][][][] multiPolygon,
                                                           boolean shouldManagePoleGeometries,
                                                           boolean checkPolygonOrientation) {
        List<double[][][]> outMultipolygons = new ArrayList<>();
        // iterate over outMultipolygons in this multipolygon
        for (int i = 0; i < multiPolygon.length; i++) {
            // Any polygon can be splited into several polygons
            outMultipolygons.addAll(sanitizePolygon(multiPolygon[i],
                                                    shouldManagePoleGeometries,
                                                    checkPolygonOrientation));
        }
        return outMultipolygons;
    }

    /**
     * Clean using the ES library the provided polygon.
     * We also simplify polygons and removes any holes
     * We can split a polygon into several polygons if they cross the date line
     *
     * @param polygon                    a single polygon
     * @param shouldManagePoleGeometries if true, polygon is calculated on spherical plan to detect antimeridian.
     * @param checkPolygonOrientation    if true reverse polygon orientation if it is not counterclockwise.
     * @return one or more polygon
     */
    public static List<double[][][]> sanitizePolygon(double[][][] polygon,
                                                     boolean shouldManagePoleGeometries,
                                                     boolean checkPolygonOrientation) {

        List<double[][][]> outMultipolygons = new ArrayList<>();
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));

        // We just keep the external line that describes this polygon. All others lines (holes) are ignored
        double[][] inExteriorRing = polygon[0];
        // Build the ES coordinates builder
        CoordinatesBuilder coordinatesBuilder = new CoordinatesBuilder();
        if (shouldManagePoleGeometries
            || !checkPolygonOrientation
            || getPolygonOrientation(inExteriorRing) == Orientation.COUNTER_CLOCKWISE) {
            for (double[] doubles : inExteriorRing) {
                coordinatesBuilder.coordinate(new Coordinate(doubles[0], doubles[1]));
            }
        } else {
            // If pole management is disabled and polygon coordinates are not in counterclockwise, reverse coordinates.
            for (int i = inExteriorRing.length - 1; i >= 0; i--) {
                coordinatesBuilder.coordinate(new Coordinate(inExteriorRing[i][0], inExteriorRing[i][1]));
            }
        }
        // ES will split polygon into several polygons if that's easier to read
        Geometry geometry = new PolygonBuilder(coordinatesBuilder, Orientation.COUNTER_CLOCKWISE).buildS4JGeometry(
            geometryFactory,
            true);

        // Save polygons into the resulting outMultipolygons
        if (geometry.getGeometryType().equals("MultiPolygon")) {
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                // As we don't have holes and we splitted polygons around the dateline
                // Result can be suprising + we can't use holes at all
                Geometry subGeometry = geometry.getGeometryN(i);
                outMultipolygons.add(esGeometryToPolygon(subGeometry));
            }
        } else if (geometry.getGeometryType().equals("Polygon")) {
            outMultipolygons.add(esGeometryToPolygon(geometry));
        }
        return outMultipolygons;
    }

    /**
     * Calculates, polygon orientation. Can be {@link Orientation#CLOCKWISE} or {@link Orientation#COUNTER_CLOCKWISE}
     */
    private static Orientation getPolygonOrientation(double[][] inCoordinates) {
        List<Coordinate> coordinates = new ArrayList<>();
        for (double[] inCoordinate : inCoordinates) {
            coordinates.add(new Coordinate(inCoordinate[0], inCoordinate[1]));
        }
        // If polygon should be eventually reversed
        // Find southest coordinate index
        int iSouthest = 0;
        for (int i = 1; i < coordinates.size(); i++) {
            if (coordinates.get(i).getY() < coordinates.get(iSouthest).getY()) {
                iSouthest = i;
            }
        }
        // Find previous and next coordinates
        Coordinate previousPoint = (iSouthest == 0) ?
            coordinates.get(coordinates.size() - 1) :
            coordinates.get(iSouthest - 1);
        Coordinate nextPoint = (iSouthest == coordinates.size() - 1) ?
            coordinates.get(0) :
            coordinates.get(iSouthest + 1);
        Coordinate southestPoint = coordinates.get(iSouthest);
        // Compute determinant between previous vector (the one finishing at southest point) and next vector (the one starting at southest point)
        Coordinate previousVector = new Coordinate(southestPoint.getX() - previousPoint.getX(),
                                                   southestPoint.getY() - previousPoint.getY());
        Coordinate nextVector = new Coordinate(nextPoint.getX() - southestPoint.getX(),
                                               nextPoint.getY() - southestPoint.getY());
        double det = previousVector.getX() * nextVector.getY() - previousVector.getY() * nextVector.getX();

        if (det < 0) {
            return Orientation.CLOCKWISE;
        } else {
            return Orientation.COUNTER_CLOCKWISE;
        }
    }

    /**
     * Create a polygon that is ecuadorian symmetric to given one
     */
    private static double[][] getSymetricPolygon(double[][] exteriorRing) {
        double[][] symetricPolygon = new double[exteriorRing.length][2];
        for (int i = 0; i < symetricPolygon.length; i++) {
            symetricPolygon[i][0] = exteriorRing[i][0];
            // Inverse latitude
            symetricPolygon[i][1] = -exteriorRing[i][1];
        }
        // Don't forget to let new polygon be a left hand polygon
        ArrayUtils.reverse(symetricPolygon);
        return symetricPolygon;
    }

    /**
     * Normalize a polygon around North Pole but not passing through it
     *
     * @param exteriorRing polygon exterior ring
     * @return modified or replaced exterior ring
     */
    private static double[][] normalizePolygonAroundNorthPole(double[][] exteriorRing) {
        // Search for north hemisphere right border longitude (max longitude) and left border longitude (immediate
        // after max)
        int idxMaxLon = eastmostAfterMaxLatitudeNorthHemisphereIndex(exteriorRing);
        int idxLeftBorderAfterMaxLon = (idxMaxLon + 1) % exteriorRing.length;
        // Be careful: if idxMaxLon is exteriorRing.length - 1, index 0 corresponds to same point
        // if index 0 is max longitude, idxMaxLon should have been 0 so we are here in case that it is the
        // same point BUT index 0 has the longitude in [-180, 0] whereas index idxMaxLon has the longitude
        // in [180, 360]: in this case MAX_LONGITUDE is 180 (not 359.99999.... because Elasticsearch doesn't love it and
        // it isn't a big problem) and MIN_LONGITIUDE is -180
        boolean sameRightAndLeftBorderPoint = idxMaxLon == (exteriorRing.length - 1);

        double rightBorderLon = exteriorRing[idxMaxLon][0];
        double leftBorderLon = exteriorRing[idxLeftBorderAfterMaxLon][0];
        double rightBorderLat = exteriorRing[idxMaxLon][1];
        double leftBorderLat = exteriorRing[idxLeftBorderAfterMaxLon][1];

        double[] rightCutPoint = new double[2];
        double[] leftCutPoint = new double[2];
        // Determine the "cut" point (dateLine or 0-meridian) depending on min and max longitude values
        if (sameRightAndLeftBorderPoint) { // tricky case see upper
            rightCutPoint[0] = 180.0;
            leftCutPoint[0] = -180.0;
            // Both latitude are equals, it is the same point
            double medianLatitude = rightBorderLat;
            rightCutPoint[1] = leftCutPoint[1] = medianLatitude;
            // Add rightCutPoint, (rightCutPoint longitude, 90°), (leftCutPoint longitude, 90°), leftCutPoint
            double[][] arrayAroundPole = new double[][] { rightCutPoint,
                                                          { rightCutPoint[0], 90.0 },
                                                          { leftCutPoint[0], 90.0 },
                                                          leftCutPoint };
            exteriorRing = ObjectArrays.concat(ObjectArrays.concat(Arrays.copyOfRange(exteriorRing, 0, idxMaxLon),
                                                                   arrayAroundPole,
                                                                   double[].class),
                                               Arrays.copyOfRange(exteriorRing, idxMaxLon, exteriorRing.length),
                                               double[].class);
            // The last point has a longitude != than first which is not correct, let's use the same
            exteriorRing[exteriorRing.length - 1] = exteriorRing[0];
        } else {
            if (rightBorderLon > 180.0) {
                // Cut meridian is 0-meridian => max longitude is 359.9999999...
                rightCutPoint[0] = MAX_CHEATED_LONGITUDE;
                leftCutPoint[0] = 0.0;
                // If both latitude are equals, median latitude is same else use Thales theorem
                double medianLatitude = rightBorderLat == leftBorderLat ?
                    rightBorderLat :
                    ((leftBorderLon * (rightBorderLat - leftBorderLat)) / ((leftBorderLon + MAX_CHEATED_LONGITUDE)
                                                                           - rightBorderLon)) + leftBorderLat;
                rightCutPoint[1] = leftCutPoint[1] = medianLatitude;
            } else { // Cut meridian is dateline => max longitude is 180
                rightCutPoint[0] = 180.0;
                leftCutPoint[0] = -180.0;
                // If both latitude are equals, median latitude is same else use Thales theorem
                double medianLatitude = rightBorderLat == leftBorderLat ?
                    rightBorderLat :
                    (((180.0 - leftBorderLon) * (rightBorderLat - leftBorderLat)) / ((180.0 - leftBorderLon)
                                                                                     + rightBorderLon
                                                                                     + 180.0)) + leftBorderLat;
                rightCutPoint[1] = leftCutPoint[1] = medianLatitude;
            }

            // Add rightCutPoint, (rightCutPoint longitude, 90°), (leftCutPoint longitude, 90°), leftCutPoint
            double[][] arrayAroundPole = new double[][] { rightCutPoint,
                                                          { rightCutPoint[0], 90.0 },
                                                          { leftCutPoint[0], 90.0 },
                                                          leftCutPoint };
            exteriorRing = ObjectArrays.concat(ObjectArrays.concat(Arrays.copyOfRange(exteriorRing, 0, idxMaxLon + 1),
                                                                   arrayAroundPole,
                                                                   double[].class),
                                               Arrays.copyOfRange(exteriorRing, idxMaxLon + 1, exteriorRing.length),
                                               double[].class);
        }
        // Algorithm hasn't take into account cut points at longitudes -180, 180, 0, ... so just remove consecutive
        // points if there are some
        exteriorRing = removeDuplicateConsecutivePoints(exteriorRing);
        return exteriorRing;
    }

    private static double[][] removeDuplicateConsecutivePoints(double[][] points) {
        List<double[]> pointList = new ArrayList<>();
        pointList.add(points[0]);
        for (int i = 1; i < points.length; i++) {
            if (!Arrays.equals(points[i], points[i - 1])) {
                pointList.add(points[i]);
            }
        }
        return pointList.toArray(new double[pointList.size()][]);
    }

    /**
     * @return index of northern point from LineString
     */
    private static int northernIndex(double[][] lineString) {
        int idxMaxLon = 0;
        for (int i = 0; i < lineString.length; i++) {
            if ((lineString[i][0] > 0.0) && (lineString[i][1] > lineString[idxMaxLon][1])) {
                idxMaxLon = i;
            }
        }
        return idxMaxLon;
    }

    /**
     * @return index of eastmost after northern point from lineString
     */
    private static int eastmostAfterMaxLatitudeNorthHemisphereIndex(double[][] lineString) {
        int startIdx = northernIndex(lineString);

        int idxMaxLon = startIdx;
        for (int i = startIdx; i < lineString.length; i++) {
            // Take the "eastmost" max (except if it is at index 0 which means it is already the rightest, think as
            // cycle array)
            if ((lineString[i][1] > 0.0) && ((lineString[i][0] > lineString[idxMaxLon][0]) || ((lineString[i][0]
                                                                                                == lineString[idxMaxLon][0])
                                                                                               && (i != 0)))) {
                idxMaxLon = i;
            }
        }
        return idxMaxLon;
    }

    private static boolean goesThroughNorthPole(double[][] lineString) {
        return Arrays.stream(lineString).anyMatch(point -> point[1] == 90.0);
    }

    private static boolean goesThroughSouthPole(double[][] lineString) {
        return Arrays.stream(lineString).anyMatch(point -> point[1] == -90.0);
    }

    /**
     * Theta is longitude from 0 to 2 * PI in radians
     */
    private static double toTheta(double lon) {
        if (lon > 0) {
            return Math.toRadians(lon);
        }
        return Math.toRadians(lon) + (2 * Math.PI);
    }

    /**
     * Phi is angle between (earth center, north pole) and point on sphere at given latitude in radians.<br/>
     * Phi is from 0 to PI
     */
    private static double toPhi(double lat) {
        return Math.toRadians(90.0 - lat);
    }

    /**
     * Transform line string (exterior ring from polygon for example) into spheric polygon
     */
    private static SphericalPolygonsSet toSphericalPolygonSet(double[][] lineString) {
        S2Point[] vertices = new S2Point[lineString.length - 1];
        for (int i = 0; i < (lineString.length - 1); i++) {
            vertices[i] = new S2Point(toTheta(lineString[i][0]), toPhi(lineString[i][1]));
        }
        return new SphericalPolygonsSet(1.e-6, vertices);
    }

    /**
     * ICriterion visitor applying specified predicate on all ICriterion tree nodes. Not leaf nodes return true if one
     * of their childs has respond true (Permit to find if tree contains a certain type of ICriterion)
     */
    private static class PredicateCriterionVisitor implements ICriterionVisitor<Boolean> {

        private final Predicate<ICriterion> predicate;

        public PredicateCriterionVisitor(Predicate<ICriterion> predicate) {
            this.predicate = predicate;
        }

        @Override
        public Boolean visitEmptyCriterion(EmptyCriterion criterion) {
            return predicate.test(criterion);
        }

        @Override
        public Boolean visitAndCriterion(AbstractMultiCriterion criterion) {
            return criterion.getCriterions().stream().anyMatch(c -> c.accept(this)) || predicate.test(criterion);
        }

        @Override
        public Boolean visitOrCriterion(AbstractMultiCriterion criterion) {
            return criterion.getCriterions().stream().anyMatch(c -> c.accept(this)) || predicate.test(criterion);
        }

        @Override
        public Boolean visitNotCriterion(NotCriterion criterion) {
            return criterion.getCriterion().accept(this) || predicate.test(criterion);
        }

        @Override
        public Boolean visitStringMatchCriterion(StringMatchCriterion criterion) {
            return predicate.test(criterion);
        }

        @Override
        public Boolean visitStringMultiMatchCriterion(StringMultiMatchCriterion criterion) {
            return predicate.test(criterion);
        }

        @Override
        public Boolean visitStringMatchAnyCriterion(StringMatchAnyCriterion criterion) {
            return predicate.test(criterion);
        }

        @Override
        public Boolean visitIntMatchCriterion(IntMatchCriterion criterion) {
            return predicate.test(criterion);
        }

        @Override
        public Boolean visitLongMatchCriterion(LongMatchCriterion criterion) {
            return predicate.test(criterion);
        }

        @Override
        public Boolean visitDateMatchCriterion(DateMatchCriterion criterion) {
            return predicate.test(criterion);
        }

        @Override
        public <U extends Comparable<? super U>> Boolean visitRangeCriterion(RangeCriterion<U> criterion) {
            return predicate.test(criterion);
        }

        @Override
        public Boolean visitDateRangeCriterion(DateRangeCriterion criterion) {
            return predicate.test(criterion);
        }

        @Override
        public Boolean visitBooleanMatchCriterion(BooleanMatchCriterion criterion) {
            return predicate.test(criterion);
        }

        @Override
        public Boolean visitPolygonCriterion(PolygonCriterion criterion) {
            return predicate.test(criterion);
        }

        @Override
        public Boolean visitBoundaryBoxCriterion(BoundaryBoxCriterion criterion) {
            return predicate.test(criterion);
        }

        @Override
        public Boolean visitCircleCriterion(CircleCriterion criterion) {
            return predicate.test(criterion);
        }

        @Override
        public Boolean visitFieldExistsCriterion(FieldExistsCriterion criterion) {
            return predicate.test(criterion);
        }
    }

    /**
     * ICriterionVisitor permitting to find a certain type of ICriterion from a ICriterion tree (a CircleCriterion for
     * example)
     *
     * @param <T> type of Icriterion to find
     */
    @SuppressWarnings("unchecked")
    private static class FinderCriterionVisitor<T extends ICriterion> implements ICriterionVisitor<T> {

        private final Class<T> clazz;

        public FinderCriterionVisitor(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public T visitEmptyCriterion(EmptyCriterion criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }

        private T visitMultiCriterion(AbstractMultiCriterion criterion) {
            if (this.clazz.isInstance(criterion)) {
                return (T) criterion;
            }
            for (ICriterion c : criterion.getCriterions()) {
                T found = c.accept(this);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }

        @Override
        public T visitAndCriterion(AbstractMultiCriterion criterion) {
            return visitMultiCriterion(criterion);
        }

        @Override
        public T visitOrCriterion(AbstractMultiCriterion criterion) {
            return visitMultiCriterion(criterion);
        }

        @Override
        public T visitNotCriterion(NotCriterion criterion) {
            if (this.clazz.isInstance(criterion)) {
                return (T) criterion;
            }
            return criterion.getCriterion().accept(this);
        }

        @Override
        public T visitStringMatchCriterion(StringMatchCriterion criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }

        @Override
        public T visitStringMultiMatchCriterion(StringMultiMatchCriterion criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }

        @Override
        public T visitStringMatchAnyCriterion(StringMatchAnyCriterion criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }

        @Override
        public T visitIntMatchCriterion(IntMatchCriterion criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }

        @Override
        public T visitLongMatchCriterion(LongMatchCriterion criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }

        @Override
        public T visitDateMatchCriterion(DateMatchCriterion criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }

        @Override
        public <U extends Comparable<? super U>> T visitRangeCriterion(RangeCriterion<U> criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }

        @Override
        public T visitDateRangeCriterion(DateRangeCriterion criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }

        @Override
        public T visitBooleanMatchCriterion(BooleanMatchCriterion criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }

        @Override
        public T visitPolygonCriterion(PolygonCriterion criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }

        @Override
        public T visitBoundaryBoxCriterion(BoundaryBoxCriterion criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }

        @Override
        public T visitCircleCriterion(CircleCriterion criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }

        @Override
        public T visitFieldExistsCriterion(FieldExistsCriterion criterion) {
            return this.clazz.isInstance(criterion) ? (T) criterion : null;
        }
    }

    /**
     * ICriterionVisitor permitting to know if geometry is nearest from distance to a point
     */
    private static class NearestFromDistanceGeometryVisitor implements IGeometryVisitor<Boolean> {

        private final Crs crs;

        private final double[] point;

        private final double distance;

        public NearestFromDistanceGeometryVisitor(double[] point, double distance, Crs crs) {
            this.crs = crs;
            this.point = point;
            this.distance = distance;
        }

        @Override
        public Boolean visitGeometryCollection(GeometryCollection geometry) {
            return null;
        }

        @Override
        public Boolean visitLineString(LineString geometry) {
            return null;
        }

        @Override
        public Boolean visitMultiLineString(MultiLineString geometry) {
            return null;
        }

        @Override
        public Boolean visitMultiPoint(MultiPoint geometry) {
            return null;
        }

        @Override
        public Boolean visitMultiPolygon(MultiPolygon geometry) {
            return null;
        }

        @Override
        public Boolean visitPoint(Point geometry) {
            return GeoHelper.getDistance(point[0],
                                         point[1],
                                         geometry.getCoordinates().getLongitude(),
                                         geometry.getCoordinates().getLatitude(),
                                         crs) < distance;
        }

        @Override
        public Boolean visitPolygon(Polygon geometry) {
            LOGGER.error("Distance from point (%f, %f): %f\n", point[0], point[1], distance);
            ArrayList<Position> positions = geometry.getCoordinates().getExteriorRing();
            Position lastPosition = null;
            for (Position position : positions) {
                if (lastPosition != null) {
                    boolean found = createIntermediatePositions(lastPosition, position).stream()
                                                                                       .anyMatch(p -> GeoHelper.getDistance(
                                                                                           point[0],
                                                                                           point[1],
                                                                                           p.getLongitude(),
                                                                                           p.getLatitude(),
                                                                                           crs) < distance);
                    if (found) {
                        return found;
                    }
                }
                lastPosition = position;
            }
            return false;
        }

        @Override
        public Boolean visitUnlocated(Unlocated geometry) {
            return null;
        }
    }

    /**
     * GeometryVisitor permitting to compute distance between a geometry and a point
     */
    private static class DistanceToPointGeometryVisitor implements IGeometryVisitor<Double> {

        private final Crs crs;

        private final double[] point;

        public DistanceToPointGeometryVisitor(double[] point, Crs crs) {
            this.crs = crs;
            this.point = point;
        }

        @Override
        public Double visitGeometryCollection(GeometryCollection geometry) {
            return null;
        }

        @Override
        public Double visitLineString(LineString geometry) {
            return null;
        }

        @Override
        public Double visitMultiLineString(MultiLineString geometry) {
            return null;
        }

        @Override
        public Double visitMultiPoint(MultiPoint geometry) {
            return null;
        }

        @Override
        public Double visitMultiPolygon(MultiPolygon geometry) {
            return null;
        }

        @Override
        public Double visitPoint(Point geometry) {
            return GeoHelper.getDistance(point[0],
                                         point[1],
                                         geometry.getCoordinates().getLongitude(),
                                         geometry.getCoordinates().getLatitude(),
                                         crs);
        }

        @Override
        public Double visitPolygon(Polygon geometry) {
            // Compute distance between given point and all points from polygon as well as 9 intermediate points from
            // all  polygon segments
            List<Position> positions = geometry.getCoordinates().getExteriorRing();
            Position lastPosition = null;
            double distance = Double.POSITIVE_INFINITY;
            for (Position position : positions) {
                if (lastPosition != null) {
                    distance = FastMath.min(distance,
                                            createIntermediatePositions(lastPosition, position).stream()
                                                                                               .mapToDouble(p -> GeoHelper.getDistance(
                                                                                                   point[0],
                                                                                                   point[1],
                                                                                                   p.getLongitude(),
                                                                                                   p.getLatitude(),
                                                                                                   crs))
                                                                                               .min()
                                                                                               .getAsDouble());
                }
                lastPosition = position;
            }
            return distance;
        }

        @Override
        public Double visitUnlocated(Unlocated geometry) {
            return null;
        }
    }
}
