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
package fr.cnes.regards.modules.authentication.dao.repository;

import fr.cnes.regards.modules.authentication.dao.entity.ServiceProviderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IServiceProviderEntityRepository
    extends JpaRepository<ServiceProviderEntity, Long>, JpaSpecificationExecutor<ServiceProviderEntity> {

    Optional<ServiceProviderEntity> findOneByName(String name);

    @Override
    default Page<ServiceProviderEntity> findAll(Pageable pageable) {
        Page<Long> idPage = findIdPage(pageable);
        List<ServiceProviderEntity> serviceProviders = findAllById(idPage.getContent());
        return new PageImpl<>(serviceProviders, idPage.getPageable(), idPage.getTotalElements());
    }

    @Query("select sp.id from ServiceProviderEntity sp")
    Page<Long> findIdPage(Pageable pageable);

    Long deleteByName(String name);
}
