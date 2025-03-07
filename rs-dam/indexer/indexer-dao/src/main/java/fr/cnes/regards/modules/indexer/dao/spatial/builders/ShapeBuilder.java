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
package fr.cnes.regards.modules.indexer.dao.spatial.builders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.Assertions;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.exception.InvalidShapeException;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;

import java.util.*;

/**
 * Basic class for building GeoJSON shapes like Polygons, Linestrings, etc
 */
public abstract class ShapeBuilder<T extends Shape, G extends org.elasticsearch.geometry.Geometry, E extends ShapeBuilder<T, G, E>>
    //implements NamedWriteable//, ToXContentObject
{

    protected static final Logger LOGGER = LogManager.getLogger(ShapeBuilder.class);

    private static final boolean DEBUG;

    static {
        // if asserts are enabled we run the debug statements even if they are not logged
        // to prevent exceptions only present if debug enabled
        DEBUG = Assertions.ENABLED;
    }

    protected final List<Coordinate> coordinates;

    public static final double DATELINE = 180;

    /**
     * coordinate at [0.0, 0.0]
     */
    public static final Coordinate ZERO_ZERO = new Coordinate(0.0, 0.0);

    // TODO how might we use JtsSpatialContextFactory to configure the context (esp. for non-geo)?
    public static final JtsSpatialContext SPATIAL_CONTEXT = JtsSpatialContext.GEO;

    public static final GeometryFactory FACTORY = SPATIAL_CONTEXT.getShapeFactory().getGeometryFactory();

    /**
     * We're expecting some geometries might cross the dateline.
     */
    protected final boolean wrapdateline = SPATIAL_CONTEXT.isGeo();

    /**
     * It's possible that some geometries in a MULTI* shape might overlap. With the possible exception of GeometryCollection,
     * this normally isn't allowed.
     */
    protected static final boolean MULTI_POLYGON_MAY_OVERLAP = false;

    /**
     * @see JtsGeometry#validate()
     */
    protected static final boolean AUTO_VALIDATE_JTS_GEOMETRY = true;

    /**
     * @see JtsGeometry#index()
     */
    protected static final boolean AUTO_INDEX_JTS_GEOMETRY = true;//may want to turn off once SpatialStrategy impls do it.

    /**
     * default ctor
     */
    protected ShapeBuilder() {
        coordinates = new ArrayList<>();
    }

    /**
     * ctor from list of coordinates
     */
    protected ShapeBuilder(List<Coordinate> coordinates) {
        if (coordinates == null || coordinates.size() == 0) {
            throw new IllegalArgumentException("cannot create point collection with empty set of points");
        }
        this.coordinates = coordinates;
    }

    @SuppressWarnings("unchecked")
    private E thisRef() {
        return (E) this;
    }

    /**
     * Add a array of coordinates to the collection
     *
     * @param coordinates array of {@link Coordinate}s to add
     * @return this
     */
    public E coordinates(Coordinate... coordinates) {
        return this.coordinates(Arrays.asList(coordinates));
    }

    /**
     * Add a collection of coordinates to the collection
     *
     * @param coordinates array of {@link Coordinate}s to add
     * @return this
     */
    public E coordinates(Collection<? extends Coordinate> coordinates) {
        this.coordinates.addAll(coordinates);
        return thisRef();
    }

    /**
     * Copy all coordinate to a new Array
     *
     * @param closed if set to true the first point of the array is repeated as last element
     * @return Array of coordinates
     */
    protected Coordinate[] coordinates(boolean closed) {
        Coordinate[] result = coordinates.toArray(new Coordinate[coordinates.size() + (closed ? 1 : 0)]);
        if (closed) {
            result[result.length - 1] = result[0];
        }
        return result;
    }

    protected JtsGeometry jtsGeometry(Geometry geom) {
        //dateline180Check is false because ElasticSearch does it's own dateline wrapping
        JtsGeometry jtsGeometry = new JtsGeometry(geom, SPATIAL_CONTEXT, false, MULTI_POLYGON_MAY_OVERLAP);
        if (AUTO_VALIDATE_JTS_GEOMETRY) {
            jtsGeometry.validate();
        }
        if (AUTO_INDEX_JTS_GEOMETRY) {
            jtsGeometry.index();
        }
        return jtsGeometry;
    }

    /**
     * Create a new Shape from this builder. Since calling this method could change the
     * defined shape. (by inserting new coordinates or change the position of points)
     * the builder looses its validity. So this method should only be called once on a builder
     *
     * @return new {@link Shape} defined by the builder
     */
    public abstract T buildS4J();

    protected static Coordinate shift(Coordinate coordinate, double dateline) {
        if (dateline == 0) {
            return coordinate;
        } else {
            return new Coordinate(-2 * dateline + coordinate.x, coordinate.y);
        }
    }

    /**
     * tracks number of dimensions for this shape
     */
    public abstract int numDimensions();

    /**
     * Calculate the intersection of a line segment and a vertical dateline.
     *
     * @param p1       start-point of the line segment
     * @param p2       end-point of the line segment
     * @param dateline x-coordinate of the vertical dateline
     * @return position of the intersection in the open range (0..1] if the line
     * segment intersects with the line segment. Otherwise this method
     * returns {@link Double#NaN}
     */
    protected static final double intersection(Coordinate p1, Coordinate p2, double dateline) {
        if (p1.x == p2.x && p1.x != dateline) {
            return Double.NaN;
        } else if (p1.x == p2.x && p1.x == dateline) {
            return 1.0;
        } else {
            final double t = (dateline - p1.x) / (p2.x - p1.x);
            if (t > 1 || t <= 0) {
                return Double.NaN;
            } else {
                return t;
            }
        }
    }

    /**
     * Calculate all intersections of line segments and a vertical line. The
     * Array of edges will be ordered asc by the y-coordinate of the
     * intersections of edges.
     *
     * @param dateline x-coordinate of the dateline
     * @param edges    set of edges that may intersect with the dateline
     * @return number of intersecting edges
     */
    protected static int intersections(double dateline, Edge[] edges) {
        int numIntersections = 0;
        assert Double.isNaN(dateline) == false;
        int maxComponent = 0;
        for (int i = 0; i < edges.length; i++) {
            Coordinate p1 = edges[i].coordinate;
            Coordinate p2 = edges[i].next.coordinate;
            assert Double.isNaN(p2.x) == false && Double.isNaN(p1.x) == false;
            edges[i].intersect = Edge.MAX_COORDINATE;

            double position = intersection(p1, p2, dateline);
            if (Double.isNaN(position) == false) {
                edges[i].intersection(position);
                numIntersections++;
                maxComponent = Math.max(maxComponent, edges[i].component);
            }
        }
        if (maxComponent > 0) {
            // we might detect polygons touching the dateline as intersections
            // Here we clean them up
            for (int i = 0; i < maxComponent; i++) {
                if (clearComponentTouchingDateline(edges, i + 1)) {
                    numIntersections--;
                }
            }
        }

        Arrays.sort(edges, INTERSECTION_ORDER);
        return numIntersections;
    }

    /**
     * Checks the number of dateline intersections detected for a component. If there is only
     * one, it clears it as it means that the component just touches the dateline.
     *
     * @param edges     set of edges that may intersect with the dateline
     * @param component The component to check
     * @return true if the component touches the dateline.
     */
    private static boolean clearComponentTouchingDateline(Edge[] edges, int component) {
        Edge intersection = null;
        for (Edge edge : edges) {
            if (edge.intersect != Edge.MAX_COORDINATE && edge.component == component) {
                if (intersection == null) {
                    intersection = edge;
                } else {
                    return false;
                }
            }
        }
        if (intersection != null) {
            intersection.intersect = Edge.MAX_COORDINATE;
        }
        return intersection != null;
    }

    /**
     * This helper class implements a linked list for {@link Coordinate}. It contains
     * fields for a dateline intersection and component id
     */
    protected static final class Edge {

        Coordinate coordinate; // coordinate of the start point

        Edge next; // next segment

        Coordinate intersect; // potential intersection with dateline

        int component = -1; // id of the component this edge belongs to

        public static final Coordinate MAX_COORDINATE = new Coordinate(Double.POSITIVE_INFINITY,
                                                                       Double.POSITIVE_INFINITY);

        protected Edge(Coordinate coordinate, Edge next, Coordinate intersection) {
            this.coordinate = coordinate;
            // use setter to catch duplicate point cases
            this.setNext(next);
            this.intersect = intersection;
            if (next != null) {
                this.component = next.component;
            }
        }

        protected Edge(Coordinate coordinate, Edge next) {
            this(coordinate, next, Edge.MAX_COORDINATE);
        }

        protected void setNext(Edge next) {
            // don't bother setting next if its null
            if (next != null) {
                // self-loop throws an invalid shape
                if (this.coordinate.equals(next.coordinate)) {
                    throw new InvalidShapeException("Provided shape has duplicate consecutive coordinates at: "
                                                    + this.coordinate);
                }
                this.next = next;
            }
        }

        /**
         * Set the intersection of this line segment to the given position
         *
         * @param position position of the intersection [0..1]
         * @return the {@link Coordinate} of the intersection
         */
        protected Coordinate intersection(double position) {
            return intersect = position(coordinate, next.coordinate, position);
        }

        protected static Coordinate position(Coordinate p1, Coordinate p2, double position) {
            if (position == 0) {
                return p1;
            } else if (position == 1) {
                return p2;
            } else {
                final double x = p1.x + position * (p2.x - p1.x);
                final double y = p1.y + position * (p2.y - p1.y);
                return new Coordinate(x, y);
            }
        }

        @Override
        public String toString() {
            return "Edge[Component=" + component + "; start=" + coordinate + " " + "; intersection=" + intersect + "]";
        }
    }

    protected static final IntersectionOrder INTERSECTION_ORDER = new IntersectionOrder();

    private static final class IntersectionOrder implements Comparator<Edge> {

        @Override
        public int compare(Edge o1, Edge o2) {
            return Double.compare(o1.intersect.y, o2.intersect.y);
        }
    }

    protected static final boolean debugEnabled() {
        return LOGGER.isDebugEnabled() || DEBUG;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o instanceof ShapeBuilder) == false) {
            return false;
        }
        ShapeBuilder<?, ?, ?> that = (ShapeBuilder<?, ?, ?>) o;
        return Objects.equals(coordinates, that.coordinates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinates);
    }

}
