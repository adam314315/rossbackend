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
package fr.cnes.regards.modules.acquisition.domain.chain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.acquisition.plugins.IProductPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ISipPostProcessingPlugin;
import fr.cnes.regards.modules.acquisition.plugins.IValidationPlugin;
import fr.cnes.regards.modules.ingest.dto.VersioningMode;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Define a product acquisition chain
 *
 * @author Marc Sordi
 */
@Entity
@Table(name = "t_acq_processing_chain")
@NamedEntityGraphs({ @NamedEntityGraph(name = "graph.acquisition.chain.complete",
                                       attributeNodes = { @NamedAttributeNode(value = "fileInfos",
                                                                              subgraph = "subgraph.file.info"),
                                                          @NamedAttributeNode(value = "lastProductAcquisitionJobInfo",
                                                                              subgraph = "graph.acquisition.chain.jobs"),
                                                          @NamedAttributeNode(value = "validationPluginConf"),
                                                          @NamedAttributeNode(value = "productPluginConf"),
                                                          @NamedAttributeNode(value = "generateSipPluginConf"),
                                                          @NamedAttributeNode(value = "postProcessSipPluginConf") },
                                       subgraphs = { @NamedSubgraph(name = "subgraph.file.info",
                                                                    attributeNodes = { @NamedAttributeNode(value = "scanDirInfo"),
                                                                                       @NamedAttributeNode(value = "scanPlugin") }),
                                                     @NamedSubgraph(name = "graph.acquisition.chain.jobs",
                                                                    attributeNodes = { @NamedAttributeNode(value = "parameters") }) }) })
public class AcquisitionProcessingChain {

    /**
     * Fixed checksum algorithm
     */
    public static final String CHECKSUM_ALGORITHM = "MD5";

    @ConfigIgnore
    @Id
    @SequenceGenerator(name = "AcquisitionChainSequence", initialValue = 1, sequenceName = "seq_acq_chain")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AcquisitionChainSequence")
    private Long id;

    @NotBlank(message = "Processing chain label is required")
    @Column(name = "label", length = 64, nullable = false)
    private String label;

    /**
     * <code>true</code> if active, <code>false</code> otherwise
     */
    @NotNull(message = "Processing chain state is required")
    @Column
    private Boolean active = false;

    @NotNull(message = "Acquisition processing mode is required")
    @Column(length = 16)
    @Enumerated(EnumType.STRING)
    private AcquisitionProcessingChainMode mode = AcquisitionProcessingChainMode.AUTO;

    /**
     * Flag to allow to run an action only once at a time
     */
    @ConfigIgnore
    @Column(updatable = false, nullable = false)
    private boolean locked = false;

    /**
     * The last activation date when an acquisition were running.
     */
    @ConfigIgnore
    @Column(name = "last_activation_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastActivationDate;

    /**
     * The cron expression of the acquisition periodicity
     */
    @Column(name = "period")
    @Pattern(regexp = "0 .*",
             message = "Invalid cron periodicity. You must start your periodicity with 0 (for 0 second) as the lowest definition is minutes. e.g: 0 * 18 * * *")
    private String periodicity;

    /**
     * Then INGEST chain name for SIP submission
     */
    @NotBlank(message = "Ingest chain is required")
    @Column(name = "ingest_chain")
    private String ingestChain;

    @Valid
    @NotNull(message = "Storage metadata is required")
    @Column(columnDefinition = "jsonb")
    @Type(value = JsonBinaryType.class,
          parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE,
                                    value = "fr.cnes.regards.modules.acquisition.domain.chain.StorageMetadataProvider") })
    private List<StorageMetadataProvider> storages;

    @Valid
    @NotNull(message = "Categories are required")
    @Column(columnDefinition = "jsonb")
    @Type(value = JsonBinaryType.class,
          parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> categories;

    @Column(name = "versioning_mode")
    @Enumerated(EnumType.STRING)
    private VersioningMode versioningMode = VersioningMode.INC_VERSION;

    /**
     * The {@link List} of files to build a product
     */
    @NotNull(message = "A processing chain must have at least one acquisition file information")
    @Size(min = 1)
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "acq_chain_id", foreignKey = @ForeignKey(name = "fk_acq_chain_id"))
    private Set<AcquisitionFileInfo> fileInfos;

    /**
     * An optional {@link PluginConfiguration} of a {@link IValidationPlugin}
     */
    @NotNull(message = "Validation plugin configuration is required")
    @ManyToOne(optional = false, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "validation_conf_id", foreignKey = @ForeignKey(name = "fk_validation_conf_id"))
    private PluginConfiguration validationPluginConf;

    /**
     * A {@link PluginConfiguration} of a {@link IProductPlugin}
     */
    @NotNull(message = "Product plugin configuration is required")
    @ManyToOne(optional = false, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "product_conf_id", foreignKey = @ForeignKey(name = "fk_product_conf_id"))
    private PluginConfiguration productPluginConf;

    /**
     * A {@link PluginConfiguration} of a {@link ISipGenerationPlugin}
     */
    @NotNull(message = "SIP generation plugin configuration is required")
    @ManyToOne(optional = false, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "generatesip_conf_id", foreignKey = @ForeignKey(name = "fk_generatesip_conf_id"))
    private PluginConfiguration generateSipPluginConf;

    /**
     * A {@link PluginConfiguration} of a {@link ISipPostProcessingPlugin}
     */
    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "postprocesssip_conf_id", foreignKey = @ForeignKey(name = "fk_postprocesssip_conf_id"))
    private PluginConfiguration postProcessSipPluginConf;

    @ConfigIgnore
    @OneToOne
    @JoinColumn(name = "acq_job_info_id", foreignKey = @ForeignKey(name = "fk_acq_job_info_id"))
    @Hidden // FIXME temporary hack : swagger generate a wrong specification, for now avoid to use it
    private JobInfo lastProductAcquisitionJobInfo;

    /**
     * Parameter to indicate if products should be stored or referenced
     * Products are stored by default
     */
    @Column(name = "products_stored")
    @NotNull(message = "Referencing system for products is required (to store or to reference")
    private boolean productsStored = true;

    /**
     * Storage plugin businessId for referenced files.
     * Used only if productsStored=true. Else storages is used.
     */
    @Column(name = "reference_location")
    private String referenceLocation;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public OffsetDateTime getLastActivationDate() {
        return lastActivationDate;
    }

    public void setLastActivationDate(OffsetDateTime lastActivationDate) {
        this.lastActivationDate = lastActivationDate;
    }

    public String getPeriodicity() {
        return periodicity;
    }

    public void setPeriodicity(String periodicity) {
        this.periodicity = periodicity;
    }

    public String getIngestChain() {
        return ingestChain;
    }

    public void setIngestChain(String ingestChain) {
        this.ingestChain = ingestChain;
    }

    public Set<AcquisitionFileInfo> getFileInfos() {
        return fileInfos;
    }

    public void setFileInfos(Set<AcquisitionFileInfo> fileInfos) {
        this.fileInfos = fileInfos;
    }

    public void addFileInfo(AcquisitionFileInfo fileInfo) {
        if (fileInfos == null) {
            fileInfos = new LinkedHashSet<>();
        }
        fileInfos.add(fileInfo);
    }

    public PluginConfiguration getGenerateSipPluginConf() {
        return generateSipPluginConf;
    }

    public void setGenerateSipPluginConf(PluginConfiguration generateSipPluginConf) {
        this.generateSipPluginConf = generateSipPluginConf;
    }

    public Optional<PluginConfiguration> getPostProcessSipPluginConf() {
        return Optional.ofNullable(postProcessSipPluginConf);
    }

    public void setPostProcessSipPluginConf(PluginConfiguration postProcessSipPluginConf) {
        this.postProcessSipPluginConf = postProcessSipPluginConf;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public AcquisitionProcessingChainMode getMode() {
        return mode;
    }

    public void setMode(AcquisitionProcessingChainMode mode) {
        this.mode = mode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PluginConfiguration getValidationPluginConf() {
        return validationPluginConf;
    }

    public void setValidationPluginConf(PluginConfiguration validationPluginConf) {
        this.validationPluginConf = validationPluginConf;
    }

    public List<StorageMetadataProvider> getStorages() {
        return storages;
    }

    public void setStorages(List<StorageMetadataProvider> storages) {
        this.storages = storages;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public PluginConfiguration getProductPluginConf() {
        return productPluginConf;
    }

    public void setProductPluginConf(PluginConfiguration productPluginConf) {
        this.productPluginConf = productPluginConf;
    }

    public Boolean isLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public JobInfo getLastProductAcquisitionJobInfo() {
        return lastProductAcquisitionJobInfo;
    }

    public void setLastProductAcquisitionJobInfo(JobInfo lastProductAcquisitionJobInfo) {
        this.lastProductAcquisitionJobInfo = lastProductAcquisitionJobInfo;
    }

    public VersioningMode getVersioningMode() {
        return versioningMode;
    }

    public void setVersioningMode(VersioningMode versioningMode) {
        this.versioningMode = versioningMode;
    }

    public boolean isProductsStored() {
        return productsStored;
    }

    public void setProductsStored(boolean productsStored) {
        this.productsStored = productsStored;
    }

    public String getReferenceLocation() {
        return referenceLocation;
    }

    public void setReferenceLocation(String referenceLocation) {
        this.referenceLocation = referenceLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AcquisitionProcessingChain that = (AcquisitionProcessingChain) o;

        if (!getLabel().equals(that.getLabel())) {
            return false;
        }
        if (!getIngestChain().equals(that.getIngestChain())) {
            return false;
        }
        if (!getFileInfos().equals(that.getFileInfos())) {
            return false;
        }
        if (!getValidationPluginConf().equals(that.getValidationPluginConf())) {
            return false;
        }
        if (!getProductPluginConf().equals(that.getProductPluginConf())) {
            return false;
        }
        if (!getGenerateSipPluginConf().equals(that.getGenerateSipPluginConf())) {
            return false;
        }
        return getPostProcessSipPluginConf() != null ?
            getPostProcessSipPluginConf().equals(that.getPostProcessSipPluginConf()) :
            that.getPostProcessSipPluginConf() == null;
    }

    @Override
    public int hashCode() {
        int result = getLabel().hashCode();
        result = 31 * result + getIngestChain().hashCode();
        result = 31 * result + getFileInfos().hashCode();
        result = 31 * result + getValidationPluginConf().hashCode();
        result = 31 * result + getProductPluginConf().hashCode();
        result = 31 * result + getGenerateSipPluginConf().hashCode();
        result = 31 * result + (getPostProcessSipPluginConf() != null ? getPostProcessSipPluginConf().hashCode() : 0);
        return result;
    }
}
