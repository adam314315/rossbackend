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
package fr.cnes.regards.framework.geojson.gson;

import com.google.gson.JsonElement;
import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.geojson.geometry.*;
import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactory;

/**
 * Gson adapter for GeoJson geometry
 *
 * @author Marc Sordi
 */
@GsonTypeAdapterFactory
public class GeometryTypeAdapterFactory extends PolymorphicTypeAdapterFactory<IGeometry> {

    public GeometryTypeAdapterFactory() {
        super(IGeometry.class, "type");
        registerSubtype(Point.class, GeoJsonType.POINT);
        registerSubtype(MultiPoint.class, GeoJsonType.MULTIPOINT);
        registerSubtype(LineString.class, GeoJsonType.LINESTRING);
        registerSubtype(MultiLineString.class, GeoJsonType.MULTILINESTRING);
        registerSubtype(Polygon.class, GeoJsonType.POLYGON);
        registerSubtype(MultiPolygon.class, GeoJsonType.MULTIPOLYGON);
        registerSubtype(GeometryCollection.class, GeoJsonType.GEOMETRY_COLLECTION);
        registerSubtype(Unlocated.class, GeoJsonType.UNLOCATED, true); // Serialize nulls
    }

    /**
     * For unlocated feature, just return null.
     */
    @Override
    protected JsonElement beforeWrite(JsonElement jsonElement, Class<?> subType) {
        if (subType == Unlocated.class) {
            return null;
        } else {
            return super.beforeWrite(jsonElement, subType);
        }
    }
}
