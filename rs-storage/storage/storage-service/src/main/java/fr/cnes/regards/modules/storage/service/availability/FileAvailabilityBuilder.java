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
package fr.cnes.regards.modules.storage.service.availability;

import fr.cnes.regards.modules.fileaccess.dto.availability.FileAvailabilityStatusDto;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import fr.cnes.regards.modules.storage.domain.database.FileReference;

import java.time.OffsetDateTime;

/**
 * Utils to build {@link FileAvailabilityStatusDto} easily
 *
 * @author Thomas GUILLOU
 **/
public final class FileAvailabilityBuilder {

    private FileAvailabilityBuilder() {
    }

    public static FileAvailabilityStatusDto buildAvailableWithoutExpiration(FileReference fileReference) {
        return new FileAvailabilityStatusDto(fileReference.getMetaInfo().getChecksum(), true, null);
    }

    public static FileAvailabilityStatusDto buildAvailable(FileReference fileReference, OffsetDateTime expirationDate) {
        return new FileAvailabilityStatusDto(fileReference.getMetaInfo().getChecksum(), true, expirationDate);
    }

    public static FileAvailabilityStatusDto buildNotAvailable(FileReference fileReference) {
        return new FileAvailabilityStatusDto(fileReference.getMetaInfo().getChecksum(), false, null);
    }

    public static FileAvailabilityStatusDto buildAvailable(CacheFile file) {
        return new FileAvailabilityStatusDto(file.getChecksum(), true, file.getExpirationDate());
    }

    public static FileAvailabilityStatusDto buildNotAvailable(CacheFile file) {
        return new FileAvailabilityStatusDto(file.getChecksum(), false, null);
    }
}
