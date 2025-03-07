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
package fr.cnes.regards.modules.acquisition.domain;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.converters.PathAttributeConverter;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.Assert;

import java.nio.file.Path;
import java.time.OffsetDateTime;

/**
 * This class represents an acquisition file.<br>
 * This file is created when detected by a scan plugin.
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 */
@Entity
@Table(name = "t_acquisition_file",
       indexes = { @Index(name = "idx_acq_file_state", columnList = "state"),
                   @Index(name = "idx_acq_file_state_file_info", columnList = "state, acq_file_info_id"),
                   @Index(name = "idx_acq_file_info", columnList = "acq_file_info_id"),
                   @Index(name = "idx_acq_file_product_id", columnList = "product_id") })
public class AcquisitionFile {

    @Id
    @SequenceGenerator(name = "AcqFileSequence", initialValue = 1, sequenceName = "seq_acq_file")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AcqFileSequence")
    private Long id;

    @NotNull(message = "File path is required")
    @Convert(converter = PathAttributeConverter.class)
    private Path filePath;

    /**
     * The data file's status
     */
    @Column(name = "state", length = 32)
    @Enumerated(EnumType.STRING)
    private AcquisitionFileState state;

    /**
     * This field is only used when acquisition file status is set to {@link AcquisitionFileState#ERROR} or {@link AcquisitionFileState#INVALID}
     */
    @Column(columnDefinition = "text")
    private String error;

    /**
     * The {@link Product} associated to the data file
     */
    @GsonIgnore
    @ManyToOne
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_acq_file_id"), updatable = false)
    private Product product;

    /**
     * Acquisition date of the data file
     */
    @NotNull(message = "Acquisition date is required")
    @Column(name = "acquisition_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime acqDate;

    @GsonIgnore
    @NotNull(message = "Acquisition file information is required")
    @ManyToOne
    @JoinColumn(name = "acq_file_info_id", foreignKey = @ForeignKey(name = "fk_acq_file_info_id"), updatable = false)
    private AcquisitionFileInfo fileInfo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AcquisitionFileState getState() {
        return state;
    }

    public void setState(AcquisitionFileState state) {
        this.state = state;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public OffsetDateTime getAcqDate() {
        return acqDate;
    }

    public void setAcqDate(OffsetDateTime acqDate) {
        this.acqDate = acqDate;
    }

    public AcquisitionFileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(AcquisitionFileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * Call method {@link #setError(String)} then call method {@link #setState(AcquisitionFileState)}
     *
     * @param msg   the error message (it is required)
     * @param state the state (it is required)
     */
    public void setErrorMsgWithState(String msg, AcquisitionFileState state) {
        Assert.hasLength(msg, "Error message is required");
        Assert.notNull(state, "State is required");

        this.setError(msg);
        this.setState(state);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (acqDate == null ? 0 : acqDate.hashCode());
        result = (prime * result) + (filePath == null ? 0 : filePath.hashCode());
        result = (prime * result) + (fileInfo == null ? 0 : fileInfo.hashCode());
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
        AcquisitionFile other = (AcquisitionFile) obj;
        if (acqDate == null) {
            if (other.acqDate != null) {
                return false;
            }
        } else if (!acqDate.equals(other.acqDate)) {
            return false;
        }
        if (fileInfo == null) {
            if (other.fileInfo != null) {
                return false;
            }
        } else if (!fileInfo.equals(other.fileInfo)) {
            return false;
        }
        return true;
    }
}
