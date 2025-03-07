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
package fr.cnes.regards.modules.feature.dto;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.geojson.AbstractFeature;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * GeoJson feature with dynamic properties based on data model definition<br/>
 * Feature id corresponds to input provider identifier
 *
 * @author Marc SORDI
 * @author Sébastien Binda
 */
public class Feature extends AbstractFeature<Set<IProperty<?>>, String> {

    /**
     * Unique feature identifer based on provider identifier with versionning
     */
    private FeatureUniformResourceName urn;

    @NotNull(message = "Feature type is required")
    private EntityType entityType;

    @NotBlank(message = "Model name is required")
    protected String model;

    @Valid
    protected FeatureHistory history;

    @Valid
    protected List<FeatureFile> files = new ArrayList<>();

    @Schema(implementation = Set.class)
    @Override
    // Override needed for swagger. Cannot use generic type with swagger
    public Set<IProperty<?>> getProperties() {
        return super.getProperties();
    }

    private boolean last = false;

    public FeatureUniformResourceName getUrn() {
        return urn;
    }

    public void setUrn(FeatureUniformResourceName urn) {
        this.urn = urn;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<FeatureFile> getFiles() {
        return files;
    }

    public void setFiles(List<FeatureFile> files) {
        this.files = files;
    }

    public boolean hasFiles() {
        return (this.files != null) && !this.files.isEmpty();
    }

    public FeatureHistory getHistory() {
        return history;
    }

    public void setHistory(FeatureHistory history) {
        this.history = history;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = (prime * result) + (entityType == null ? 0 : entityType.hashCode());
        result = (prime * result) + (files == null ? 0 : files.hashCode());
        result = (prime * result) + (model == null ? 0 : model.hashCode());
        result = (prime * result) + (urn == null ? 0 : urn.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Feature other = (Feature) obj;
        if (entityType != other.entityType) {
            return false;
        }
        if (files == null) {
            if (other.files != null) {
                return false;
            }
        } else if (!files.equals(other.files)) {
            return false;
        }
        if (model == null) {
            if (other.model != null) {
                return false;
            }
        } else if (!model.equals(other.model)) {
            return false;
        }
        if (urn == null) {
            if (other.urn != null) {
                return false;
            }
        } else if (!urn.equals(other.urn)) {
            return false;
        }
        return true;
    }

    public static Feature build(String id,
                                String owner,
                                @Nullable FeatureUniformResourceName urn,
                                IGeometry geometry,
                                EntityType entityType,
                                String model) {
        Feature feature = new Feature();
        feature.setUrn(urn);
        feature.setEntityType(entityType);
        feature.setModel(model);
        feature.setGeometry(geometry);
        feature.setId(id);
        feature.setProperties(new HashSet<>());
        feature.setHistory(FeatureHistory.build(owner));
        return feature;
    }

    public Feature withHistory(String createdBy, @Nullable String updatedBy, @Nullable String deletedBy) {
        this.setHistory(FeatureHistory.build(createdBy, updatedBy, deletedBy));
        return this;
    }

    public Feature withHistory(String createdBy) {
        this.setHistory(FeatureHistory.build(createdBy));
        return this;
    }

    public Feature withProperties(Set<IProperty<?>> properties) {
        this.setProperties(properties);
        return this;
    }

    public Feature withUpdatedBy(String updatedBy) {
        if (this.history != null) {
            this.getHistory().setUpdatedBy(updatedBy);
        }
        return this;
    }

    public Feature withProperties(IProperty<?>... properties) {
        this.setProperties(new HashSet<>(Arrays.asList(properties)));
        return this;
    }

    public Feature withFiles(FeatureFile... files) {
        this.setFiles(Lists.newArrayList(files));
        return this;
    }

    public void addProperty(IProperty<?> property) {
        if (this.properties == null) {
            this.properties = new HashSet<>();
        }
        this.properties.add(property);
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isLast() {
        return last;
    }
}