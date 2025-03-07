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
import fr.cnes.regards.framework.geojson.coordinates.Position;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJson Point representation
 *
 * @author Marc Sordi
 */
public class Point extends AbstractGeometry<Position> {

    public Point() {
        super(GeoJsonType.POINT);
    }

    @Override
    public <T> T accept(IGeometryVisitor<T> visitor) {
        return visitor.visitPoint(this);
    }

    @Override
    public String toString() {
        return "POINT ( " + getCoordinates().toString() + " )";
    }

    public double[] toArray() {
        return coordinates.size() == 2 ?
            new double[] { coordinates.getLongitude(), coordinates.getLatitude() } :
            new double[] { coordinates.getLongitude(), coordinates.getLatitude(), coordinates.getAltitude().get() };
    }

    /**
     * Create a Point from array  { longitude, latitude }
     * <B>NOTE: the goal of this method is to ease creation/transformation/computation of geometries so no check is
     * done concerning input values.</B>
     */
    public static Point fromArray(double[] lonLat) {
        Point point = new Point();
        point.coordinates = Position.fromArray(lonLat);
        return point;
    }

}
