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
package fr.cnes.regards.modules.file.packager.dao;

import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link FileInBuildingPackage}
 *
 * @author Thibaud Michaudel
 **/
public interface FileInBuildingPackageRepository extends JpaRepository<FileInBuildingPackage, Long> {

    Page<FileInBuildingPackage> findByStatusOrderByStorageAscStorageSubdirectoryAsc(FileInBuildingPackageStatus status,
                                                                                    Pageable page);

    Page<FileInBuildingPackage> findByPackageReferenceId(Long packageId, Pageable page);
}
