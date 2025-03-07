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
package fr.cnes.regards.modules.acquisition.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductsPage;
import fr.cnes.regards.modules.acquisition.domain.chain.*;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChains;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Acquisition processing service interface
 *
 * @author Marc Sordi
 */
public interface IAcquisitionProcessingService {

    /**
     * List all acquisition chains
     *
     * @param pageable pagination filter
     * @return list of all acquisition chains
     */
    Page<AcquisitionProcessingChain> getAllChains(Pageable pageable);

    /**
     * Retrieve a processing chain according to its identifier.
     *
     * @param id {@link AcquisitionProcessingChain} identifier
     * @return {@link AcquisitionProcessingChain}
     * @throws ModuleException if error occurs.
     */
    AcquisitionProcessingChain getChain(Long id) throws ModuleException;

    /**
     * Retrieve all processing chains
     *
     * @return all chains fully loaded
     */
    List<AcquisitionProcessingChain> getFullChains();

    /**
     * Retrieve all processing chains by page
     *
     * @return all chains fully loaded
     */
    Page<AcquisitionProcessingChain> getFullChains(Pageable pageable);

    /**
     * Update an existing processing chain
     *
     * @param processingChain the updated processing chain
     * @return updated processing chain
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain updateChain(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Create a new acquisition processing chain
     *
     * @param processingChain the processing chain
     * @return registered processing chain
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain createChain(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Patch an existing processing chain with new values for active and state
     */
    AcquisitionProcessingChain patchStateAndMode(Long chainId, UpdateAcquisitionProcessingChain payload)
        throws ModuleException;

    /**
     * Patch several existing processing chain with provided values for active and state
     */
    List<AcquisitionProcessingChain> patchChainsStateAndMode(UpdateAcquisitionProcessingChains payload)
        throws ModuleException;

    /**
     * Delete an inactive processing chain according to its identifier
     *
     * @param id {@link AcquisitionProcessingChain} identifier
     * @throws ModuleException if error occurs.
     */
    void deleteChain(Long id) throws ModuleException;

    /**
     * Check if a {@link AcquisitionProcessingChain} deletion is pending
     *
     * @return boolean
     */
    boolean isDeletionPending(AcquisitionProcessingChain chain);

    /**
     * Lock processing chain
     *
     * @param id {@link AcquisitionProcessingChain} identifier
     */
    void lockChain(Long id);

    /**
     * Unlock processing chain
     *
     * @param id {@link AcquisitionProcessingChain} identifier
     */
    void unlockChain(Long id);

    /**
     * Start all automatic chains according to several conditions
     */
    void startAutomaticChains(OffsetDateTime lastCheckDate, OffsetDateTime currentDate);

    /**
     * Start a chain manually
     *
     * @param processingChainId identifier of the chain to start
     * @param session           optional, replace the name of the acquisition session
     * @param onlyErrors,       launch session only to retry generation errors.
     * @return started processing chain
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain startManualChain(Long processingChainId, Optional<String> session, boolean onlyErrors)
        throws ModuleException;

    /**
     * Stop a chain regardless of its mode.
     *
     * @param processingChainId identifier of the chain to stop
     * @throws ModuleException if error occurs!
     */
    void stopChainJobs(Long processingChainId) throws ModuleException;

    /**
     * Check if a chain is stopped and cleaned
     *
     * @param processingChainId identifier of the stopping chain
     * @return true if all jobs are stopped and related products are cleaned
     * @throws ModuleException if error occurs!
     */
    boolean isChainJobStoppedAndCleaned(Long processingChainId) throws ModuleException;

    /**
     * Stop a chain and clean all inconsistencies after all jobs are aborted
     *
     * @param processingChainId identifier of the chain to stop
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain stopAndCleanChain(Long processingChainId) throws ModuleException;

    /**
     * Schedule a deletion job for the given parameters.
     *
     * @param processingChainLabel chain label to delete products
     * @param session              Optional Session name to delete
     * @param deleteChain          True to delete the {@link AcquisitionProcessingChain} after products deletion.
     * @throws ModuleException if error occurs!
     */
    void scheduleProductDeletion(String processingChainLabel, Optional<String> session, boolean deleteChain)
        throws ModuleException;

    /**
     * Schedule a deletion job for the given parameters.
     *
     * @param processingChainId chain id to delete products
     * @param session           Optional Session name to delete
     * @param deleteChain       True to delete the {@link AcquisitionProcessingChain} after products deletion.
     * @throws ModuleException if error occurs!
     */
    void scheduleProductDeletion(Long processingChainId, Optional<String> session, boolean deleteChain)
        throws ModuleException;

    /**
     * Scan and register detected files for specified {@link AcquisitionProcessingChain}
     *
     * @param processingChain processing chain
     * @param session         name of the acquisition processing session
     * @throws ModuleException if error occurs!
     */
    void scanAndRegisterFiles(AcquisitionProcessingChain processingChain, String session) throws ModuleException;

    /**
     * Register multiple files in one transaction
     *
     * @param filePaths    paths of the files to register
     * @param info         related file info
     * @param scanningDate last modification date of the directory
     * @param limit        maximum number of files to register
     * @return number of registered files
     */
    RegisterFilesResponse registerFilesBatch(Iterator<Path> filePaths,
                                             AcquisitionFileInfo info,
                                             Optional<OffsetDateTime> scanningDate,
                                             int limit,
                                             String session,
                                             String sessionOwner,
                                             String fileExtensionToFilter) throws ModuleException;

    /**
     * Register multiple files by creating multiple transactions by batch
     */
    long registerFiles(Iterator<Path> filePathsIt,
                       AcquisitionFileInfo fileInfo,
                       ScanDirectoryInfo scanDir,
                       Optional<OffsetDateTime> scanningDate,
                       String session,
                       String sessionOwner,
                       String fileExtensionToFilter) throws ModuleException;

    /**
     * Register a new file in one transaction
     *
     * @param filePath     path of the file to register
     * @param info         related file info
     * @param scanningDate reference date used to launch scan plugin
     * @return true if really registered
     */
    boolean registerFile(Path filePath, AcquisitionFileInfo info, Optional<OffsetDateTime> scanningDate)
        throws ModuleException;

    /**
     * Manage new registered file : prepare or fulfill products and schedule SIP generation as soon as possible
     *
     * @return number of scheduled products
     */
    long manageRegisteredFiles(AcquisitionProcessingChain processingChain, String session) throws ModuleException;

    /**
     * Same action as {@link #manageRegisteredFiles(AcquisitionProcessingChain, String)} but in a new transaction and by page
     */
    ProductsPage manageRegisteredFilesByPage(AcquisitionProcessingChain processingChain, String session)
        throws ModuleException;

    /**
     * Restart jobs in {@link ProductSIPState#SCHEDULED_INTERRUPTED} for processing chain
     */
    void restartInterruptedJobs(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Retry SIP generation for products in {@link ProductSIPState#GENERATION_ERROR} or
     * {@link ProductSIPState#INGESTION_FAILED}
     */
    void retrySIPGeneration(AcquisitionProcessingChain processingChain, Optional<String> sessionToRetry);

    /**
     * Build summaries list of {@link AcquisitionProcessingChain}s.
     * Each summary allow to monitor chain progress.
     *
     * @param label    {@link String} optional search parameter on {@link AcquisitionProcessingChain}s label
     * @param runnable {@link Boolean} optional search parameter on {@link AcquisitionProcessingChain}s running
     */
    Page<AcquisitionProcessingChainMonitor> buildAcquisitionProcessingChainSummaries(String label,
                                                                                     Boolean runnable,
                                                                                     AcquisitionProcessingChainMode mode,
                                                                                     Pageable pageable);

    /**
     * Handle {@link fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob} errors
     */
    void handleProductAcquisitionError(JobInfo jobInfo);

    List<AcquisitionProcessingChain> findAllBootableAutomaticChains();

    List<AcquisitionProcessingChain> findByModeAndActiveTrueAndLockedFalse(AcquisitionProcessingChainMode manual);

    void relaunchErrors(String chainName, String session) throws ModuleException;

    List<String> getExecutionBlockers(AcquisitionProcessingChain chain);

    boolean hasExecutionBlockers(AcquisitionProcessingChain chain, boolean doNotify);

    boolean canBeStarted(AcquisitionProcessingChainMonitor chainMonitor);

    boolean canBeStarted(AcquisitionProcessingChain chain);

    void handleProductAcquisitionAborted(JobInfo jobInfo);
}
