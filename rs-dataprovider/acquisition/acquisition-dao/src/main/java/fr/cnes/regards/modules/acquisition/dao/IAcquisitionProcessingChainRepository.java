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
package fr.cnes.regards.modules.acquisition.dao;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * {@link AcquisitionProcessingChain} repository
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 */
@Repository
public interface IAcquisitionProcessingChainRepository
    extends JpaRepository<AcquisitionProcessingChain, Long>, JpaSpecificationExecutor<AcquisitionProcessingChain> {

    Long countById(Long id);

    default boolean existsChain(Long id) {
        return countById(id) == 1;
    }

    @EntityGraph(value = "graph.acquisition.chain.complete", type = EntityGraph.EntityGraphType.LOAD)
    AcquisitionProcessingChain findCompleteById(Long id);

    /**
     * Find all active ,not running and unlocked processing chain for a specified mode
     *
     * @param mode chain processing mode
     * @return all chains
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    List<AcquisitionProcessingChain> findByModeAndActiveTrueAndLockedFalse(AcquisitionProcessingChainMode mode);

    /**
     * Find all active and not running processing chain for a specified mode
     *
     * @param mode chain processing mode
     * @return all chains
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    List<AcquisitionProcessingChain> findByModeAndActiveTrue(AcquisitionProcessingChainMode mode);

    /**
     * @return all automatic chains that might be started
     */
    default List<AcquisitionProcessingChain> findAllBootableAutomaticChains() {
        return findByModeAndActiveTrue(AcquisitionProcessingChainMode.AUTO);
    }

    @Query(
        "select chain.validationPluginConf from AcquisitionProcessingChain chain,PluginConfiguration conf where chain.id = ?1 and chain.validationPluginConf.id = conf.id")
    Optional<PluginConfiguration> findOneValidationPlugin(Long chainId);

    @Query(
        "select chain.productPluginConf from AcquisitionProcessingChain chain,PluginConfiguration conf where chain.id = ?1 and chain.productPluginConf.id = conf.id")
    Optional<PluginConfiguration> findOneProductPlugin(Long chainId);

    @Query(
        "select chain.generateSipPluginConf from AcquisitionProcessingChain chain,PluginConfiguration conf where chain.id = ?1 and chain.generateSipPluginConf.id = conf.id")
    Optional<PluginConfiguration> findOneGenerateSipPlugin(Long chainId);

    @Query(
        "select chain.postProcessSipPluginConf from AcquisitionProcessingChain chain,PluginConfiguration conf where chain.id = ?1 and chain.postProcessSipPluginConf.id = conf.id")
    Optional<PluginConfiguration> findOnePostProcessSipPlugin(Long chainId);

    @Modifying
    @Query("update AcquisitionProcessingChain chain set chain.locked = ?1 where chain.id = ?2")
    int setLocked(Boolean isLocked, Long chainId);

    List<AcquisitionProcessingChain> findByLabel(String label);

    @EntityGraph(value = "graph.acquisition.chain.complete", type = EntityGraph.EntityGraphType.LOAD)
    @Override
    List<AcquisitionProcessingChain> findAll();

    @EntityGraph(value = "graph.acquisition.chain.complete", type = EntityGraph.EntityGraphType.LOAD)
    @Override
    Page<AcquisitionProcessingChain> findAll(Pageable pageable);

    @EntityGraph(value = "graph.acquisition.chain.complete", type = EntityGraph.EntityGraphType.LOAD)
    @Override
    Page<AcquisitionProcessingChain> findAll(Specification<AcquisitionProcessingChain> specification,
                                             Pageable pageable);

}
