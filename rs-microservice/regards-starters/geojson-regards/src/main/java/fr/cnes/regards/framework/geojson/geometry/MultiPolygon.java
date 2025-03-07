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
package fr.cnes.regards.framework.geojson.geometry;

import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.geojson.coordinates.PolygonPositions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJson MultiPolygon representation<br/>
 *
 * @author Marc Sordi
 */
public class MultiPolygon extends AbstractGeometry<List<PolygonPositions>> {

    public MultiPolygon() {
        super(GeoJsonType.MULTIPOLYGON);
        coordinates = new ArrayList<>();
    }

    @Override
    public <T> T accept(IGeometryVisitor<T> visitor) {
        return visitor.visitMultiPolygon(this);
    }

    public double[][][][] toArray() {
        return coordinates.stream().map(PolygonPositions::toArray).toArray(double[][][][]::new);
    }

    /**
     * Create a MultiPolygon from array { { { { longitude, latitude }, {}, ... } } }
     * <B>NOTE: the goal of this method is to ease creation/transformation/computation of geometries so no check is
     * done concerning input values.</B>
     */
    public static MultiPolygon fromArray(double[][][][] lonLatsArrays) {
        MultiPolygon multiPolygon = new MultiPolygon();
        multiPolygon.coordinates.addAll(Arrays.asList(Arrays.stream(lonLatsArrays)
                                                            .map(PolygonPositions::fromArray)
                                                            .toArray(PolygonPositions[]::new)));
        return multiPolygon;
    }
}
