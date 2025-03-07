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

import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;

/**
 * Specific feature properties for data objects
 *
 * @author Marc Sordi
 */
public class DataObjectFeature extends EntityFeature {

    /**
     * Session key information
     */
    private String sessionOwner;

    private String session;

    public DataObjectFeature(UniformResourceName id, String providerId, String label) {
        super(id, providerId, EntityType.DATA, label);
    }

    public DataObjectFeature(UniformResourceName id,
                             String providerId,
                             String label,
                             String sessionOwner,
                             String session,
                             String model) {
        super(id, providerId, EntityType.DATA, label);
        this.sessionOwner = sessionOwner;
        this.session = session;
        this.model = model;
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
}
