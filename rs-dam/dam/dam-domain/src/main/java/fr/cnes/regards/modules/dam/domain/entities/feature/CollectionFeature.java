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
package fr.cnes.regards.modules.dam.domain.entities.feature;

import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;

import java.util.UUID;

/**
 * Specific feature properties for collections
 *
 * @author Marc Sordi
 */
public class CollectionFeature extends EntityFeature {

    /**
     * Deserialization constructor
     */
    protected CollectionFeature() {
        super(null, null, EntityType.COLLECTION, null);
    }

    public CollectionFeature(String tenant, String providerId, String label) {
        super(new OaisUniformResourceName(OAISIdentifier.AIP,
                                          EntityType.COLLECTION,
                                          tenant,
                                          UUID.randomUUID(),
                                          1,
                                          null,
                                          null), providerId, EntityType.COLLECTION, label);
    }

    public CollectionFeature(UniformResourceName id, String providerId, String label) {
        super(id, providerId, EntityType.COLLECTION, label);
    }
}
