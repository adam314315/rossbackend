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
package fr.cnes.regards.modules.model.domain.attributes.restriction;

import com.google.gson.annotations.JsonAdapter;
import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.modules.model.domain.adapter.RestrictionJsonAdapterFactory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * @author msordi
 */
@Entity
@Table(name = "t_restriction")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype", length = 16)
@SequenceGenerator(name = "restrictionSequence", initialValue = 1, sequenceName = "seq_restriction")
@JsonAdapter(RestrictionJsonAdapterFactory.class)
public abstract class AbstractRestriction implements IRestriction, IIdentifiable<Long> {

    /**
     * Internal identifier
     */
    @Id
    @ConfigIgnore
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "restrictionSequence")
    protected Long id;

    /**
     * Attribute restriction type
     */
    @Column(length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    protected RestrictionType type;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    @Override
    @NotNull(message = "Restriction type cannot be null")
    public RestrictionType getType() {
        return type;
    }

    public void setType(RestrictionType pType) {
        type = pType;
    }

}
