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

import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

/**
 * @author kevin
 */
public class FeatureCreationSessionMetadata extends FeatureSessionMetadata {

    /**
     * If we override previous version or not
     */
    private boolean override = false;

    /**
     * If urn is provided and associated feature already exists update the existing feature with parameters of given one
     */
    private boolean updateIfExists = false;

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        this.override = override;
    }

    public boolean isUpdateIfExists() {
        return updateIfExists;
    }

    public void setUpdateIfExists(boolean updateIfExists) {
        this.updateIfExists = updateIfExists;
    }

    /**
     * Build feature metadata
     *
     * @param sessionOwner Owner of the session
     * @param session      session
     * @param override     if we override previous version
     * @param storages     storage metadata
     */
    public static FeatureCreationSessionMetadata build(String sessionOwner,
                                                       String session,
                                                       PriorityLevel priority,
                                                       boolean override,
                                                       boolean updateIfExists,
                                                       StorageMetadata... storages) {
        return FeatureCreationSessionMetadata.build(sessionOwner,
                                                    session,
                                                    priority,
                                                    Arrays.asList(storages),
                                                    override,
                                                    updateIfExists);
    }

    /**
     * Build feature metadata
     *
     * @param sessionOwner Owner of the session
     * @param session      session
     * @param storages     storage metadata
     * @param override     if we override previous version
     */
    public static FeatureCreationSessionMetadata build(String sessionOwner,
                                                       String session,
                                                       PriorityLevel priority,
                                                       List<StorageMetadata> storages,
                                                       boolean override,
                                                       boolean updateIfExists) {
        Assert.hasLength(sessionOwner, MISSING_SESSION_OWNER);
        Assert.hasLength(session, MISSING_SESSION);
        Assert.notNull(storages, MISSING_STORAGE_METADATA);
        Assert.notNull(priority, MISSING_PRIORITY_LEVEL);
        FeatureCreationSessionMetadata m = new FeatureCreationSessionMetadata();
        m.setSessionOwner(sessionOwner);
        m.setSession(session);
        m.setStorages(storages);
        m.setPriority(priority);
        m.setOverride(override);
        m.setUpdateIfExists(updateIfExists);
        return m;
    }
}
