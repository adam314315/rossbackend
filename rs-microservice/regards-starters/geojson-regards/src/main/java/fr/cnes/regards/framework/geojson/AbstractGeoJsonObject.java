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
package fr.cnes.regards.framework.geojson;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Arrays;
import java.util.Optional;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJsonObject common model
 *
 * @author Marc Sordi
 */
public abstract class AbstractGeoJsonObject {

    @NotNull
    @Schema(description = "Type of the feature entity.", example = "Feature", allowableValues = {"Feature",
                                                                                           "FeatureCollection"})
    protected String type;

    /**
     * Optional bounding box
     */
    @Schema(description = "Geometry bounding box. List of points coordinates [xmin, ymin, xmax, ymax]",
            example = "[-122.70, 45.51, -122.64, 45.53]")
    protected Double[] bbox;

    /**
     * Optional coordinate reference system. If not specified, WGS84 is considered as the default CRS.<br/>
     * CRS is not in RFC 7946 -August 2016.
     */
    @Schema(description ="Coordinate Reference System",defaultValue = "WGS84", example = "WGS84")
    protected String crs;

    protected AbstractGeoJsonObject(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public Optional<Double[]> getBbox() {
        return Optional.ofNullable(bbox);
    }

    public void setBbox(Double[] bbox) {
        this.bbox = bbox;
    }

    public Optional<String> getCrs() {
        return Optional.ofNullable(crs);
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + Arrays.hashCode(bbox);
        result = (prime * result) + ((crs == null) ? 0 : crs.hashCode());
        result = (prime * result) + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractGeoJsonObject other = (AbstractGeoJsonObject) obj;
        if (!Arrays.equals(bbox, other.bbox)) {
            return false;
        }
        if (crs == null) {
            if (other.crs != null) {
                return false;
            }
        } else if (!crs.equals(other.crs)) {
            return false;
        }
        return (type.equals(other.type));
    }
}
