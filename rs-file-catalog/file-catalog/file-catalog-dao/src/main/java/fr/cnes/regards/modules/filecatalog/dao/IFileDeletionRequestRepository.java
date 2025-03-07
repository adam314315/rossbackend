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
package fr.cnes.regards.modules.filecatalog.dao;

import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.filecatalog.domain.request.FileDeletionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

/**
 * JPA Repository to handle access to {@link FileDeletionRequest} entities.
 *
 * @author Thibaud Michaudel
 */
public interface IFileDeletionRequestRepository extends JpaRepository<FileDeletionRequest, Long> {

    // --------
    //  SEARCH
    // --------

    boolean existsByStorageAndFileReferenceMetaInfoChecksumAndStatusIn(String storage,
                                                                       String checksum,
                                                                       Set<FileRequestStatus> runningStatus);

    @Query("select storage from FileDeletionRequest where status = :status")
    Set<String> findStoragesByStatus(@Param("status") FileRequestStatus status);

    Page<FileDeletionRequest> findByStorageAndStatusAndIdGreaterThan(String storage,
                                                                     FileRequestStatus status,
                                                                     Long maxId,
                                                                     Pageable page);

    Long countByStorageAndStatus(String storage, FileRequestStatus status);

    // --------
    //  DELETE
    // --------

    void deleteByStorage(String storageLocationId);

    void deleteByStorageAndStatus(String storageLocationId, FileRequestStatus fileRequestStatus);

}
