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
package fr.cnes.regards.modules.dam.domain.entities;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.validator.PastOrNow;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.urn.converters.UrnConverter;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.indexer.domain.IDocFiles;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.spatial.ILocalizable;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.dto.properties.AbstractProperty;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.dto.properties.ObjectProperty;
import io.vavr.control.Option;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Base entity feature decorator
 *
 * @param <F> represents the decorated entity feature
 * @author Léo Mieulet
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 */

@Entity
@Table(name = "t_entity",
       indexes = { @Index(name = "idx_entity_ipId", columnList = "ipId") },
       uniqueConstraints = @UniqueConstraint(name = "uk_entity_ipId", columnNames = { "ipId" }))
@NamedEntityGraph(name = "graph.full.abstract.entity",
                  attributeNodes = { @NamedAttributeNode(value = "tags"), @NamedAttributeNode(value = "groups") })
@DiscriminatorColumn(name = "dtype", length = 10)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class AbstractEntity<F extends EntityFeature> implements IIndexable, IDocFiles, ILocalizable {

    public static final String TAGS_MUST_NOT_BE_NULL_OR_EMPTY = "Tags must not be null or empty";

    /**
     * Entity id for SGBD purpose mainly and REST request
     */
    @Id
    @SequenceGenerator(name = "EntitySequence", initialValue = 1, sequenceName = "seq_entity")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EntitySequence")
    private Long id;

    /**
     * Information Package ID for REST request
     */
    @Column(nullable = false, length = UniformResourceName.MAX_SIZE)
    @Convert(converter = UrnConverter.class)
    @Valid
    private UniformResourceName ipId;

    /**
     * time at which the entity was created
     */
    @PastOrNow(message = "The creationDate must be in the past or now")
    @Column(name = "creation_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    protected OffsetDateTime creationDate;

    /**
     * last time the entity was updated
     */
    @PastOrNow(message = "The lastUpdate date must be in the past or now")
    @Column(name = "update_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    protected OffsetDateTime lastUpdate;

    /**
     * model that this entity is respecting
     */
    @NotNull(message = "The Model must not be null")
    @ManyToOne
    @JoinColumn(name = "model_id",
                foreignKey = @ForeignKey(name = "fk_entity_model_id"),
                nullable = false,
                updatable = false)
    protected Model model;

    /**
     * Input tags: a tag is either an URN to a collection (ie a direct access collection) or a word without business
     * meaning<br/>
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "t_entity_tag",
                     joinColumns = @JoinColumn(name = "entity_id"),
                     foreignKey = @ForeignKey(name = "fk_entity_tag_entity_id"))
    @Column(name = "value", length = 200)
    protected Set<String> tags = new HashSet<>();

    /**
     * Computed indirect access groups.<br/>
     * This is a set of group names that the entity can reach (access groups are positionned on datasets and then added
     * to collections that tag the dataset and then added to collections that tag collections containing groups)
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "t_entity_group",
                     joinColumns = @JoinColumn(name = "entity_id"),
                     foreignKey = @ForeignKey(name = "fk_entity_group_entity_id"))
    @Column(name = "name", length = 200)
    protected Set<String> groups = new HashSet<>();

    /**
     * feature.geometry projection on WGS84 crs
     */
    @Valid
    @Type(value = JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    protected IGeometry wgs84 = IGeometry.unlocated();

    /**
     * Raw entity feature with minimum fuss
     */
    @Valid
    @NotNull(message = "Feature is required")
    @Column(columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    protected F feature;

    protected AbstractEntity(Model model, F feature) {
        this.model = model;
        this.feature = feature;
        if (this.feature != null) {
            this.ipId = feature.getId();
            this.feature.setModel(model.getName());
        }
    }

    protected AbstractEntity() {
        this(null, null);
    }

    @Override
    public String getDocId() {
        return feature.getId().toString();
    }

    @Override
    public String getType() {
        return this.feature.getEntityType().toString();
    }

    @Override
    public Multimap<DataType, DataFile> getFiles() {
        return feature.getFiles();
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return feature id
     */
    public UniformResourceName getIpId() {
        return feature.getId();
    }

    /**
     * Set the feature id
     */
    public void setIpId(UniformResourceName ipId) {
        this.ipId = ipId;
        // Propagate to feature
        Option.of(feature).peek(f -> f.setId(ipId));
    }

    /**
     * Get an immutable copy of tags. To modify tag, use {@link #setTags(Set)} or {@link #addTags(String...)} or
     * {@link #removeTags(Collection)}
     *
     * @return tags
     */
    public ImmutableSet<String> getTags() {
        return ImmutableSet.copyOf(tags);
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
        // Propagate to feature
        Option.of(feature).peek(f -> f.setTags(tags));
    }

    public void addTags(Collection<String> tags) {
        this.tags.addAll(tags);
        // Propagate to feature
        Option.of(feature).peek(f -> f.addTags(tags));
    }

    public void addTags(String... tags) {
        Assert.notEmpty(tags, TAGS_MUST_NOT_BE_NULL_OR_EMPTY);
        this.tags.addAll(Arrays.asList(tags));
        // Propagate to feature
        Option.of(feature).peek(f -> f.addTags(tags));
    }

    public void removeTags(Collection<String> tags) {
        Assert.notEmpty(tags, TAGS_MUST_NOT_BE_NULL_OR_EMPTY);
        this.tags.removeAll(tags);
        // Propagate to feature
        Option.of(feature).peek(f -> f.removeTags(tags));
    }

    public void clearTags() {
        this.tags.clear();
        // Propagate to feature
        Option.of(feature).peek(f -> f.getTags().clear());
    }

    /**
     * Get an immutable copy of feature properties.
     * If this set should be modified, please use addProperty or removeProperty
     *
     * @return {@link AbstractProperty}s
     */
    public ImmutableSet<IProperty<?>> getProperties() {
        return ImmutableSet.copyOf(feature.getProperties());
    }

    /**
     * Get a mutable copy of property paths.
     *
     * @return properties path
     */
    public Set<String> getMutableCopyOfPropertiesPaths() {
        Set<String> propertiesPaths = new HashSet<>();
        for (IProperty<?> prop : feature.getProperties()) {
            // Fragment
            if (prop instanceof ObjectProperty) {
                String fragmentName = prop.getName();
                ((ObjectProperty) prop).getValue()
                                       .forEach(fProp -> propertiesPaths.add(fragmentName + "." + fProp.getName()));
            } else {
                propertiesPaths.add(prop.getName());

            }
        }
        return propertiesPaths;
    }

    /**
     * Add feature property
     *
     * @param property {@link AbstractProperty}
     */
    public void addProperty(IProperty<?> property) {
        feature.addProperty(property);
    }

    public void removeProperty(IProperty<?> property) {
        feature.removeProperty(property);
    }

    public IProperty<?> getProperty(String name) {
        return feature.getProperty(name);
    }

    /**
     * Set the properties
     */
    public void setProperties(Set<IProperty<?>> attributes) {
        Option.of(feature).peek(f -> f.setProperties(attributes));
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public String getProviderId() {
        return feature.getProviderId();
    }

    public void setProviderId(String providerId) {
        Option.of(feature).peek(f -> f.setProviderId(providerId));
    }

    @Override
    public String getLabel() {
        return feature.getLabel();
    }

    public void setLabel(String label) {
        Option.of(feature).peek(f -> f.setLabel(label));
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IGeometry> T getNormalizedGeometry() {
        return (T) feature.getNormalizedGeometry();
    }

    public void setNormalizedGeometry(IGeometry geometry) {
        Option.of(feature).peek(f -> f.setNormalizedGeometry(geometry));
    }

    @SuppressWarnings("unchecked")
    public <T extends IGeometry> T getGeometry() {
        return (T) feature.getGeometry();
    }

    public void setGeometry(IGeometry geometry) {
        Option.of(feature).peek(f -> f.setGeometry(geometry));
    }

    @SuppressWarnings("unchecked")
    public <T extends IGeometry> T getWgs84() {
        return (T) wgs84;
    }

    public void setWgs84(IGeometry wgs84) {
        this.wgs84 = wgs84;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        // CHECKSTYLE:OFF
        result = (prime * result) + (getIpId() == null ? 0 : getIpId().hashCode());
        // CHECKSTYLE:ON
        return result;
    }

    @Override
    public boolean equals(Object pObj) {
        if (this == pObj) {
            return true;
        }
        if (pObj == null) {
            return false;
        }
        if (getClass() != pObj.getClass()) {
            return false;
        }
        AbstractEntity<?> other = (AbstractEntity<?>) pObj;
        if (getIpId() == null) {
            if (other.getIpId() != null) {
                return false;
            }
        } else if (!getIpId().equals(other.getIpId())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "AbstractEntity [lastUpdate="
               + lastUpdate
               + ", creationDate="
               + creationDate
               + ", id="
               + id
               + ", ipId="
               + getIpId()
               + ", providerId="
               + getProviderId()
               + ", label="
               + getLabel()
               + ", attributes="
               + getProperties()
               + ", model="
               + model
               + "]";
    }

    public F getFeature() {
        return feature;
    }

    public void setFeature(F feature) {
        this.feature = feature;
    }

    public boolean isLast() {
        return feature.isLast();
    }

    public void setLast(boolean last) {
        Option.of(feature).peek(f -> f.setLast(last));
    }

    public UniformResourceName getVirtualId() {
        return feature.getVirtualId();
    }

    public void setVirtualId() {
        Option.of(feature).peek(f -> f.setVirtualId());
    }

    public void removeVirtualId() {
        feature.removeVirtualId();
    }

    public Integer getVersion() {
        return feature.getVersion();
    }

    public void setVersion(Integer version) {
        Option.of(feature).peek(f -> f.setVersion(version));
    }

}
