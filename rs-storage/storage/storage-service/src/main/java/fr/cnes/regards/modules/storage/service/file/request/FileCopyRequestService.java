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
package fr.cnes.regards.modules.storage.service.file.request;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.request.FileCopyDto;
import fr.cnes.regards.modules.fileaccess.plugin.domain.INearlineStorageLocation;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesCopyEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.storage.dao.IFileCopyRequestRepository;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storage.service.StorageJobsPriority;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import fr.cnes.regards.modules.storage.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.job.FileCopyRequestsCreatorJob;
import fr.cnes.regards.modules.storage.service.session.SessionNotifier;
import net.javacrumbs.shedlock.core.LockConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

/**
 * Service to handle {@link FileCopyRequest}s.
 * Those requests are created when a file reference need to be restored physically thanks to an existing {@link INearlineStorageLocation} plugin.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileCopyRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCopyRequestService.class);

    public static final String COPY_PROCESS_LOCK = "copy-requests-lock";

    public static final String COPY_REQUEST_CREATOR_LOCK = "copy-requests-creator-lock";

    private static final long COPY_LOCK_TIME_TO_LIVE_IN_SECONDS = 60;

    @Autowired
    private IFileCopyRequestRepository copyRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private FileReferenceEventPublisher publisher;

    @Autowired
    private FileCacheRequestService fileCacheReqService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private RequestsGroupService reqGrpService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private RequestStatusService reqStatusService;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Value("${regards.storage.copy.requests.days.before.expiration:5}")
    private Integer nbDaysBeforeExpiration;

    /**
     * Initialize new copy request from events.
     */
    public void copy(Collection<FilesCopyEvent> items) {
        for (FilesCopyEvent item : items) {
            copy(item.getFiles(), item.getGroupId());
            reqGrpService.granted(item.getGroupId(),
                                  FileRequestType.COPY,
                                  item.getFiles().size(),
                                  getRequestExpirationDate());
        }
    }

    /**
     * Initialize new copy requests for a given group identifier
     */
    public void copy(Collection<FileCopyDto> requests, String groupId) {
        for (FileCopyDto request : requests) {
            // copy the file
            copy(request, groupId);
        }
    }

    /**
     * Handle a {@link FileCopyDto}.<br>
     * If a copy request with the same parameters already exists, this method does not creates a new one.<br>
     * If the file to copy is well referenced, then a new copy request is created.<br>
     * The copy request should be handled next by the {@link FileRequestScheduler#handleFileCopyRequests()} method.
     *
     * @param requestDto {@link FileCopyDto} to handle.
     * @param groupId    request group identifier.
     * @return {@link FileCopyRequest} created if any.
     */
    public Optional<FileCopyRequest> copy(FileCopyDto requestDto, String groupId) {
        // notify the copy request to the session agent
        String sessionOwner = requestDto.getSessionOwner();
        String session = requestDto.getSession();
        this.sessionNotifier.incrementCopyRequests(sessionOwner, session);

        // Check a same request already exists
        Optional<FileCopyRequest> request = copyRepository.findOneByMetaInfoChecksumAndStorage(requestDto.getChecksum(),
                                                                                               requestDto.getStorage());
        if (request.isPresent()) {
            return Optional.of(handleAlreadyExists(requestDto, request.get(), groupId));
        } else {
            // get file meta info to copy
            Set<FileReference> refs = fileRefService.search(requestDto.getChecksum());
            if (refs.isEmpty()) {
                String message = String.format(
                    "File copy request refused for file %s to %s storage location. Cause file does not exists in any known storage location.",
                    requestDto.getChecksum(),
                    requestDto.getStorage());
                LOGGER.warn("[COPY REQUEST] {}", message);
                notificationClient.notify(message,
                                          "File copy request refused",
                                          NotificationLevel.WARNING,
                                          DefaultRole.PROJECT_ADMIN);
                // notify denied request to the session agent
                this.sessionNotifier.incrementDeniedRequests(sessionOwner, session);
            } else {
                // Check if destination file already exists
                if (refs.stream().anyMatch(r -> r.getLocation().getStorage().equals(requestDto.getStorage()))) {
                    FileReference existingfileRef = refs.stream()
                                                        .filter(r -> r.getLocation()
                                                                      .getStorage()
                                                                      .equals(requestDto.getStorage()))
                                                        .findFirst()
                                                        .get();
                    String message = String.format("File to copy %s already exists for destination storage %s",
                                                   requestDto.getChecksum(),
                                                   requestDto.getStorage());
                    LOGGER.debug("[COPY REQUEST] {}", message);
                    publisher.copySuccess(existingfileRef, message, groupId);
                    reqGrpService.requestSuccess(groupId,
                                                 FileRequestType.COPY,
                                                 requestDto.getChecksum(),
                                                 requestDto.getStorage(),
                                                 requestDto.getSubDirectory(),
                                                 existingfileRef.getLazzyOwners(),
                                                 existingfileRef);
                } else {
                    LOGGER.debug("[COPY REQUEST] Create copy request for group {}", groupId);
                    FileCopyRequest newRequest = copyRepository.save(new FileCopyRequest(groupId,
                                                                                         refs.stream()
                                                                                             .findFirst()
                                                                                             .get()
                                                                                             .getMetaInfo(),
                                                                                         requestDto.getSubDirectory(),
                                                                                         requestDto.getStorage(),
                                                                                         sessionOwner,
                                                                                         session));
                    request = Optional.of(newRequest);
                    // notify request is running
                    this.sessionNotifier.incrementRunningRequests(sessionOwner, session);
                }
            }
        }
        return request;
    }

    /**
     * Handle the case of a copy requests already exists.<br>
     *
     * @param requestDto {@link FileCopyDto} new request to handle.
     * @param request    {@link FileCopyRequest} existing request.
     * @param newGroupId New request group idenfitier.
     * @return updated {@link FileCopyRequest}
     */
    private FileCopyRequest handleAlreadyExists(FileCopyDto requestDto, FileCopyRequest request, String newGroupId) {
        if (request.getStatus() == FileRequestStatus.ERROR) {
            // decrement the number of errors to the session agent
            String sessionOwner = requestDto.getSessionOwner();
            String session = requestDto.getSession();
            // Decrement error on previous session
            this.sessionNotifier.decrementErrorRequests(request.getSessionOwner(), request.getSession());
            // Increment running request on new session
            this.sessionNotifier.incrementRunningRequests(sessionOwner, session);
            // set the new status
            request.setStatus(reqStatusService.getNewStatus(request, Optional.empty()));
            request.setFileCacheGroupId(newGroupId);
            return update(request);
        }
        return request;
    }

    /**
     * Schedule availability requests for all copy requests.<br>
     *
     * @param status status of copy requests to schedule.
     */
    public void scheduleCopyRequests(FileRequestStatus status) {
        try {
            lockingTaskExecutors.executeWithLock(new CopyRequestTask(fileCacheReqService,
                                                                     copyRepository,
                                                                     reqGrpService,
                                                                     status),
                                                 new LockConfiguration(Instant.now(),
                                                                       COPY_PROCESS_LOCK,
                                                                       Duration.ofSeconds(COPY_LOCK_TIME_TO_LIVE_IN_SECONDS),
                                                                       Duration.ZERO));
        } catch (Throwable e) {
            LOGGER.trace("[FILES COPY REQUEST SERVICE] Copy process delayed. A copy process is already running.", e);
        }
    }

    /**
     * Handle many {@link FileCopyDto} to copy files to a given storage location.
     *
     * @param requests copy requests
     * @param groupId  business request identifier
     */
    public void handle(Collection<FileCopyDto> requests, String groupId) {
        for (FileCopyDto request : requests) {
            copy(request, groupId);
        }
    }

    /**
     * Handle copy request success.
     *
     * @param request    {@link FileCopyRequest} succeeded request.
     * @param newFileRef {@link FileReference} new file reference. Copy of the original file reference.
     */
    public void handleSuccess(FileCopyRequest request, FileReference newFileRef) {
        String successMessage = String.format("File %s (checksum: %s) successfully copied in %s storage location",
                                              request.getMetaInfo().getFileName(),
                                              request.getMetaInfo().getChecksum(),
                                              request.getStorage());
        LOGGER.debug("[COPY SUCCESS] {}", successMessage);

        // Check if associated cache file is always present
        Optional<CacheFile> oCf = cacheService.findByChecksum(request.getMetaInfo().getChecksum());
        if (oCf.isPresent()) {
            // If it is present, check if an other availability request was used for this
            if (!oCf.get().getGroupIds().stream().anyMatch(id -> !id.equals(request.getFileCacheGroupId()))) {
                // If not, delete the cache file
                cacheService.delete(oCf.get());
            }
        }
        publisher.copySuccess(newFileRef, successMessage, request.getGroupId());
        reqGrpService.requestSuccess(request.getGroupId(),
                                     FileRequestType.COPY,
                                     request.getMetaInfo().getChecksum(),
                                     request.getStorage(),
                                     request.getStorageSubDirectory(),
                                     newFileRef.getLazzyOwners(),
                                     newFileRef);

        // Delete the copy request
        copyRepository.delete(request);

        // notify session agent of the request success
        this.sessionNotifier.decrementRunningRequests(request.getSessionOwner(), request.getSession());
    }

    /**
     * Handle copy request error.
     *
     * @param request {@link FileCopyRequest} error request.
     */
    public void handleError(FileCopyRequest request, String errorCause) {
        LOGGER.error("[COPY ERROR] Error copying file {} (checksum: {}) to {} storage location. Cause : {}",
                     request.getMetaInfo().getFileName(),
                     request.getMetaInfo().getChecksum(),
                     request.getStorage(),
                     errorCause);
        // Update copy request to error status
        request.setStatus(FileRequestStatus.ERROR);
        request.setErrorCause(errorCause);
        update(request);
        publisher.copyError(request, errorCause);
        reqGrpService.requestError(request.getGroupId(),
                                   FileRequestType.COPY,
                                   request.getMetaInfo().getChecksum(),
                                   request.getStorage(),
                                   null,
                                   Sets.newHashSet(),
                                   errorCause);
        // notify session agent of the error success
        String sessionOwner = request.getSessionOwner();
        String session = request.getSession();
        this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
        this.sessionNotifier.incrementErrorRequests(sessionOwner, session);
    }

    /**
     * Search for a {@link FileCopyRequest} for the given checksum and given storage copy destination.
     *
     * @return {@link FileCopyRequest} if any
     */
    @Transactional(readOnly = true)
    public Optional<FileCopyRequest> search(String checksum, String storage) {
        return copyRepository.findOneByMetaInfoChecksumAndStorage(checksum, storage);
    }

    /**
     * Search for a {@link FileCopyRequest} for the given checksum.
     *
     * @return {@link FileCopyRequest} if any
     */
    @Transactional(readOnly = true)
    public Set<FileCopyRequest> search(String checksum) {
        return copyRepository.findByMetaInfoChecksum(checksum);
    }

    /**
     * Search for a {@link FileCopyRequest} associated to the given {@link FileReferenceEvent}.
     *
     * @return {@link FileCopyRequest} if any
     */
    public Optional<FileCopyRequest> search(FileReferenceEvent event) {
        Optional<FileCopyRequest> req = Optional.empty();
        Iterator<String> it;
        // At this point there can be only one group Id
        if (event.getGroupIds().size() == 1) {
            switch (event.getType()) {
                case AVAILABLE:
                case AVAILABILITY_ERROR:
                    it = event.getGroupIds().iterator();
                    while (it.hasNext() && !req.isPresent()) {
                        req = copyRepository.findOneByMetaInfoChecksumAndFileCacheGroupId(event.getChecksum(),
                                                                                          it.next());
                    }
                    break;
                case STORED:
                case STORE_ERROR:
                    it = event.getGroupIds().iterator();
                    while (it.hasNext() && !req.isPresent()) {
                        // There is one storage request per copy request
                        req = copyRepository.findOneByFileStorageGroupId(it.next());
                    }
                    break;
                case DELETED_FOR_OWNER:
                case FULLY_DELETED:
                case DELETION_ERROR:
                default:
                    break;
            }
        }
        return req;
    }

    /**
     * Update in database the given {@link FileCopyRequest}.
     */
    public FileCopyRequest update(FileCopyRequest request) {
        Assert.notNull(request, "FileCopyRequest to update can not be null.");
        Assert.notNull(request.getId(), "FileCopyRequestto update. Identifier can not be null.");
        return copyRepository.save(request);
    }

    /**
     * Schedule a job to create {@link FileCopyRequest}s for the given criterion
     */
    public JobInfo scheduleJob(String storageLocationId,
                               String sourcePath,
                               String destinationStorageId,
                               Optional<String> destinationPath,
                               Collection<String> types,
                               String sessionOwner,
                               String session) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(FileCopyRequestsCreatorJob.STORAGE_LOCATION_SOURCE_ID_PARMETER_NAME,
                                        storageLocationId));
        parameters.add(new JobParameter(FileCopyRequestsCreatorJob.STORAGE_LOCATION_DESTINATION_ID_PARMETER_NAME,
                                        destinationStorageId));
        parameters.add(new JobParameter(FileCopyRequestsCreatorJob.SOURCE_PATH_PARMETER_NAME, sourcePath));
        parameters.add(new JobParameter(FileCopyRequestsCreatorJob.DESTINATION_PATH_PARMETER_NAME,
                                        destinationPath.orElse("")));
        parameters.add(new JobParameter(FileCopyRequestsCreatorJob.FILE_TYPES_PARMETER_NAME, types));
        parameters.add(new JobParameter(FileCopyRequestsCreatorJob.SESSION_OWNER_PARMETER_NAME, sessionOwner));
        parameters.add(new JobParameter(FileCopyRequestsCreatorJob.SESSION_PARMETER_NAME, session));

        JobInfo jobInfo = jobInfoService.createAsQueued(new JobInfo(false,
                                                                    StorageJobsPriority.FILE_COPY_JOB,
                                                                    parameters,
                                                                    authResolver.getUser(),
                                                                    FileCopyRequestsCreatorJob.class.getName()));
        LOGGER.debug("[COPY REQUESTS] Job scheduled to copy files from {}(dir={}) to {}(dir={}) for types {}.",
                     storageLocationId,
                     sourcePath,
                     destinationStorageId,
                     destinationPath.orElse(""),
                     types);
        return jobInfo;
    }

    /**
     *
     */
    public void deleteByStorage(String storageLocationId, Optional<FileRequestStatus> status) {
        if (status.isPresent()) {
            copyRepository.deleteByStorageAndStatus(storageLocationId, status.get());
        } else {
            copyRepository.deleteByStorage(storageLocationId);
        }
    }

    /**
     * Inform if for the given storage a deletion process is running
     *
     * @return boolean
     */
    public boolean isCopyRunning(String storage) {
        boolean isRunning = false;
        // Does a deletion job exists ?
        isRunning = jobInfoService.retrieveJobsCount(FileCopyRequestsCreatorJob.class.getName(),
                                                     JobStatus.PENDING,
                                                     JobStatus.QUEUED,
                                                     JobStatus.RUNNING,
                                                     JobStatus.TO_BE_RUN) > 0;
        if (!isRunning) {
            isRunning = copyRepository.existsByStorageAndStatusIn(storage,
                                                                  Sets.newHashSet(FileRequestStatus.RUNNING_STATUS));
        }
        return isRunning;
    }

    /**
     * Check if a copy request exists for the given file reference
     */
    public boolean existsByChecksumAndStatusIn(String checksum, Collection<FileRequestStatus> status) {
        return copyRepository.existsByMetaInfoChecksumAndStatusIn(checksum, status);
    }

    /**
     *
     */
    public boolean isFileCopyRunning(Collection<String> cheksums) {
        return copyRepository.existsByMetaInfoChecksumInAndStatusIn(cheksums, FileRequestStatus.RUNNING_STATUS);
    }

    /**
     * Retrieve expiration date for deletion request
     */
    public OffsetDateTime getRequestExpirationDate() {
        if ((nbDaysBeforeExpiration != null) && (nbDaysBeforeExpiration > 0)) {
            return OffsetDateTime.now().plusDays(nbDaysBeforeExpiration);
        } else {
            return null;
        }
    }
}
