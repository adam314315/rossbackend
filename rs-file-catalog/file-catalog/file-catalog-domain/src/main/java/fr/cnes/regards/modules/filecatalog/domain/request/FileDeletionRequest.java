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
package fr.cnes.regards.modules.filecatalog.domain.request;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileDeletionRequestDto;
import fr.cnes.regards.modules.filecatalog.domain.FileLocation;
import fr.cnes.regards.modules.filecatalog.domain.FileReference;
import jakarta.persistence.*;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;

/**
 * Database definition of the table containing the requests for files deletion.
 *
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_file_deletion_request",
       indexes = { @Index(name = "idx_file_deletion_request", columnList = "storage"),
                   @Index(name = "idx_file_deletion_grp", columnList = "group_id"),
                   @Index(name = "idx_file_deletion_file_ref", columnList = "file_reference") },
       uniqueConstraints = { @UniqueConstraint(name = "uk_t_file_deletion_request_file_reference",
                                               columnNames = { "file_reference" }) })
public class FileDeletionRequest {

    @Id
    @SequenceGenerator(name = "fileDeletionSequence", initialValue = 1, sequenceName = "seq_file_deletion")
    @GeneratedValue(generator = "fileDeletionSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Business identifier to regroup file requests.
     */
    @Column(name = "group_id", nullable = false, length = 128)
    private String groupId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileRequestStatus status = FileRequestStatus.TO_DO;

    @Column(nullable = false, length = FileLocation.STORAGE_MAX_LENGTH)
    private String storage;

    @JoinColumn(name = "file_reference", foreignKey = @ForeignKey(name = "fk_t_file_deletion_request_t_file_reference"))
    @OneToOne
    private FileReference fileReference;

    @Column(name = "force_delete")
    private boolean forceDelete = false;

    @Column(name = "error_cause", length = 512)
    private String errorCause;

    @Column(name = "creation_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private final OffsetDateTime creationDate;

    @Column(name = "job_id")
    private String jobId;

    @Column(name = "session_owner")
    private String sessionOwner;

    @Column(name = "session_name")
    private String session;

    public FileDeletionRequest() {
        super();
        this.creationDate = OffsetDateTime.now();
    }

    public FileDeletionRequest(FileReference fileReference, String groupId, String sessionOwner, String session) {
        this(fileReference, false, groupId, sessionOwner, session);
    }

    public FileDeletionRequest(FileReference fileReference,
                               boolean forceDelete,
                               String groupId,
                               String sessionOwner,
                               String session) {
        super();
        Assert.notNull(fileReference, "File reference to delete cannot be null");
        Assert.notNull(fileReference.getId(), "File reference to delete identifier cannot be null");
        Assert.notNull(fileReference.getLocation(), "Unable to delete a file with no location");
        Assert.notNull(fileReference.getLocation().getStorage(), "Unable to delete a file with no location storage.");

        this.groupId = groupId;
        this.storage = fileReference.getLocation().getStorage();
        this.fileReference = fileReference;
        this.forceDelete = forceDelete;
        this.sessionOwner = sessionOwner;
        this.session = session;
        this.creationDate = OffsetDateTime.now();

    }

    private FileDeletionRequest(Long id,
                                String groupId,
                                FileRequestStatus status,
                                String storage,
                                FileReference fileReference,
                                boolean forceDelete,
                                String errorCause,
                                OffsetDateTime creationDate,
                                String jobId,
                                String sessionOwner,
                                String session) {
        this.id = id;
        this.groupId = groupId;
        this.status = status;
        this.storage = storage;
        this.fileReference = fileReference;
        this.forceDelete = forceDelete;
        this.errorCause = errorCause;
        this.creationDate = creationDate;
        this.jobId = jobId;
        this.sessionOwner = sessionOwner;
        this.session = session;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FileReference getFileReference() {
        return fileReference;
    }

    public void setFileReference(FileReference fileReference) {
        this.fileReference = fileReference;
    }

    public FileRequestStatus getStatus() {
        return status;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public void setErrorCause(String errorCause) {
        if (errorCause != null && errorCause.length() > 512) {
            this.errorCause = errorCause.substring(0, 511);
        } else {
            this.errorCause = errorCause;
        }
    }

    public void setStatus(FileRequestStatus status) {
        this.status = status;
    }

    public boolean isForceDelete() {
        return forceDelete;
    }

    public void setForceDelete(boolean forceDelete) {
        this.forceDelete = forceDelete;
    }

    public String getGroupId() {
        return groupId;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FileDeletionRequest other = (FileDeletionRequest) obj;
        if (id == null) {
            return other.id == null;
        } else {
            return id.equals(other.id);
        }
    }

    public FileDeletionRequestDto toDto() {
        return new FileDeletionRequestDto(id,
                                          groupId,
                                          status,
                                          storage,
                                          fileReference.toDtoWithoutOwners(),
                                          forceDelete,
                                          errorCause,
                                          creationDate,
                                          jobId,
                                          sessionOwner,
                                          session);
    }

    public static FileDeletionRequest fromDto(FileDeletionRequestDto dto) {
        return new FileDeletionRequest(dto.getId(),
                                       dto.getGroupId(),
                                       dto.getStatus(),
                                       dto.getStorage(),
                                       FileReference.fromDto(dto.getFileReference()),
                                       dto.isForceDelete(),
                                       dto.getErrorCause(),
                                       dto.getCreationDate(),
                                       dto.getJobId(),
                                       dto.getSessionOwner(),
                                       dto.getSession());
    }

}
