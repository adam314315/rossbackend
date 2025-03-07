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
import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.validator.LineStringConstraints;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJson LineString representation
 * <br/>
 * LineString MUST be an array of <b>two or more positions</b>.<br/>
 *
 * @author Marc Sordi
 */
@LineStringConstraints
public class LineString extends MultiPoint {

    public LineString() {
        super(GeoJsonType.LINESTRING);
    }

    @Override
    public <T> T accept(IGeometryVisitor<T> visitor) {
        return visitor.visitLineString(this);
    }

    @Override
    public String toString() {
        return "LINE STRING ( " + getCoordinates().toString() + " )";
    }

    /**
     * Create a LineString from array  { { longitude, latitude }, {}, ... }
     * <B>NOTE: the goal of this method is to ease creation/transformation/computation of geometries so no check is
     * done concerning input values.</B>
     */
    public static LineString fromArray(double[][] lonLats) {
        LineString lineString = new LineString();
        lineString.coordinates = Positions.fromArray(lonLats);
        return lineString;
    }
}
