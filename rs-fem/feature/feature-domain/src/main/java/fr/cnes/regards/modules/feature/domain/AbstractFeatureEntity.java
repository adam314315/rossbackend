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
package fr.cnes.regards.modules.feature.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.dto.urn.converter.FeatureUrnConverter;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;

@MappedSuperclass
public class AbstractFeatureEntity {

    @Id
    @SequenceGenerator(name = "featureSequence", initialValue = 1, sequenceName = "seq_feature", allocationSize = 1000)
    @GeneratedValue(generator = "featureSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, length = FeatureUniformResourceName.MAX_SIZE)
    @Convert(converter = FeatureUrnConverter.class)
    private FeatureUniformResourceName urn;

    @Column(name = "previous_version_urn", length = FeatureUniformResourceName.MAX_SIZE)
    @Convert(converter = FeatureUrnConverter.class)
    private FeatureUniformResourceName previousVersionUrn;

    @Column(length = 128, name = "session_owner", nullable = false)
    private String sessionOwner;

    @Column(length = 128, name = "session_name", nullable = false)
    private String session;

    @Column(columnDefinition = "jsonb", name = "feature")
    @Type(JsonBinaryType.class)
    @Valid
    private Feature feature;

    @Column(name = "last_update", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    @NotNull
    private OffsetDateTime lastUpdate;

    @Column(name = "creation_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    @NotNull
    private OffsetDateTime creationDate;

    @Column(name = "provider_id", nullable = false)
    @NotBlank(message = "Provider id is required")
    private String providerId;

    @Column(name = "version", nullable = false)
    @NotNull
    private Integer version;

    @Column(name = "model", nullable = false)
    @NotNull
    private String model;

    @Column(name = "dissemination_pending")
    private boolean disseminationPending;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public void setSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public FeatureUniformResourceName getUrn() {
        return urn;
    }

    public void setUrn(FeatureUniformResourceName urn) {
        this.urn = urn;
    }

    public FeatureUniformResourceName getPreviousVersionUrn() {
        return previousVersionUrn;
    }

    public void setPreviousVersionUrn(FeatureUniformResourceName previousVersionUrn) {
        this.previousVersionUrn = previousVersionUrn;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isDisseminationPending() {
        return disseminationPending;
    }

    public void setDisseminationPending(boolean disseminationPending) {
        this.disseminationPending = disseminationPending;
    }

}
