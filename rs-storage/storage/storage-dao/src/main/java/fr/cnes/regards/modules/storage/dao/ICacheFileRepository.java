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
package fr.cnes.regards.modules.storage.dao;

import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * JPA Interface to access {@link CacheFile}s entities.
 *
 * @author Sébastien Binda
 */
public interface ICacheFileRepository extends JpaRepository<CacheFile, Long> {

    /**
     * Get all {@link CacheFile}s for the given {@link String}s of checksums.
     *
     * @param checksums {@link String}s
     * @return {@link CacheFile}s
     */
    Set<CacheFile> findAllByChecksumIn(Set<String> checksums);

    /**
     * Retrieve a {@link CacheFile} by his checksum
     *
     * @return {@link Optional} {@link CacheFile}
     */
    Optional<CacheFile> findOneByChecksum(String checksum);

    Page<CacheFile> findAllByInternalCacheTrue(Pageable pageable);

    /**
     * Retrieve all {@link CacheFile}s with expiration date before the given {@link OffsetDateTime} in internal cache
     *
     * @param pEpirationDate {@link OffsetDateTime}
     * @return {@link Set}<{@link CacheFile}
     */
    Page<CacheFile> findByExpirationDateBeforeAndInternalCacheTrue(OffsetDateTime pEpirationDate, Pageable pageable);

    /**
     * Remove a {@link CacheFile} by his checksum.
     *
     * @param checksum {@link String}
     */
    void removeByChecksum(String checksum);

    @Query("SELECT COALESCE(SUM(cf.fileSize), 0) FROM CacheFile cf WHERE cf.internalCache=true")
    Long getTotalFileSizeInternalCache();

    long countCacheFileByInternalCacheTrue();
}
