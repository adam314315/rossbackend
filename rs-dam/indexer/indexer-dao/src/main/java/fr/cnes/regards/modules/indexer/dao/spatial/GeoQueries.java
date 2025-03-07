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

import fr.cnes.regards.framework.geojson.coordinates.PolygonPositions;
import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.geometry.MultiPolygon;
import fr.cnes.regards.framework.geojson.geometry.Polygon;
import fr.cnes.regards.modules.indexer.dao.spatial.builders.CoordinatesBuilder;
import fr.cnes.regards.modules.indexer.dao.spatial.builders.PolygonBuilder;
import fr.cnes.regards.modules.indexer.dao.spatial.builders.ShapeBuilder;
import fr.cnes.regards.modules.indexer.domain.criterion.PolygonCriterion;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Geo queries utility class
 *
 * @author oroussel
 */
public final class GeoQueries {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoQueries.class);

    private GeoQueries() {
    }

    /**
     * ComputeShapeBuilder from polygon criterion depending on polygon nature
     */
    public static ShapeBuilder<?, ?, ?> computeShapeBuilder(PolygonCriterion criterion) {
        // Only shell can be taken into account (external emprise)
        Polygon polygon = Polygon.fromArray(criterion.getCoordinates());
        MultiPolygon shell = GeoHelper.normalizePolygon(polygon);

        List<PolygonPositions> coordinates = shell.getCoordinates();

        if (coordinates.size() > 1) {
            LOGGER.warn("Unexpected number of polygons");
        }
        CoordinatesBuilder coordBuilder = new CoordinatesBuilder();

        Positions exteriorRing = coordinates.get(0).getExteriorRing();
        double[][] coords = exteriorRing.toArray();
        for (double[] point : coords) {
            coordBuilder.coordinate(new Coordinate(point[0], point[1]));
        }
        return new PolygonBuilder(coordBuilder);
    }

}
