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

import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.converters.SipStateConverter;
import fr.cnes.regards.modules.ingest.dto.ISipState;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Christophe Mertz
 */
@Entity
@Table(name = "t_acquisition_product",
       indexes = { @Index(name = "idx_acq_processing_chain", columnList = "processing_chain_id"),
                   @Index(name = "idx_acq_product_name", columnList = "product_name"),
                   @Index(name = "idx_acq_product_sip_state", columnList = "sip_state"),
                   @Index(name = "idx_acq_product_state", columnList = "product_state") },
       uniqueConstraints = { @UniqueConstraint(name = "uk_acq_product_ipId", columnNames = "ip_id"),
                             @UniqueConstraint(name = "uk_acq_product_name", columnNames = "product_name") })

@NamedEntityGraph(name = "graph.product.complete",
                  attributeNodes = { @NamedAttributeNode(value = "fileList"),
                                     @NamedAttributeNode(value = "lastSIPGenerationJobInfo",
                                                         subgraph = "graph.product.jobs"),
                                     @NamedAttributeNode(value = "lastPostProductionJobInfo",
                                                         subgraph = "graph.product.jobs") },
                  subgraphs = { @NamedSubgraph(name = "graph.product.jobs",
                                               attributeNodes = { @NamedAttributeNode(value = "parameters") }) })
@NamedEntityGraph(name = "graph.product.with.scan.folder",
                  attributeNodes = { @NamedAttributeNode(value = "fileList", subgraph = "subgraph.acquisition.file") },
                  subgraphs = { @NamedSubgraph(name = "subgraph.acquisition.file",
                                               attributeNodes = { @NamedAttributeNode(value = "fileInfo",
                                                                                      subgraph = "subgraph.scan.dir") }),
                                @NamedSubgraph(name = "subgraph.scan.dir",
                                               attributeNodes = { @NamedAttributeNode(value = "scanDirInfo") }) })
public class Product {

    private static final String MISSING_SESSION_ERROR = "Session is required";

    /**
     * {@link List} of file include in the {@link Product}
     */
    @GsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_product_id"))
    private final Set<AcquisitionFile> fileList = new HashSet<>();

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ProductSequence", initialValue = 1, sequenceName = "seq_product")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ProductSequence")
    private Long id;

    /**
     * The {@link Product} status
     */
    @Column(name = "product_state", length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductState state;

    @Column(name = "sip_state", length = 32, nullable = false)
    @Convert(converter = SipStateConverter.class)
    private ISipState sipState;

    /**
     * This field is only used when sip state is set to an error state
     */
    @Column(columnDefinition = "text")
    private String error;

    @Column(name = "last_update", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdate;

    /**
     * The product name
     */
    @NotBlank
    @Column(name = "product_name", length = 128)
    private String productName;

    /**
     * The session name that create the current product
     */
    @NotBlank(message = MISSING_SESSION_ERROR)
    @Column(name = "session", length = 128)
    private String session;

    @GsonIgnore
    @NotNull
    @ManyToOne
    @JoinColumn(name = "processing_chain_id",
                foreignKey = @ForeignKey(name = "fk_processing_chain_id"),
                updatable = false)
    private AcquisitionProcessingChain processingChain;

    @Column(columnDefinition = "jsonb", name = "json_sip")
    @Type(JsonBinaryType.class)
    private SIPDto sip;

    /**
     * The unique ingest IP identifier : only available if product SIP has been properly submitted to INGEST
     * microservice.
     */
    @Column(name = "ip_id", length = UniformResourceName.MAX_SIZE)
    private String ipId;

    @OneToOne
    @JoinColumn(name = "sip_gen_job_info_id", foreignKey = @ForeignKey(name = "fk_sip_gen_job_info_id"))
    @Hidden // FIXME temporary hack : swagger generate a wrong specification, for now avoid to use it
    private JobInfo lastSIPGenerationJobInfo;

    @OneToOne
    @JoinColumn(name = "post_prod_job_info_id", foreignKey = @ForeignKey(name = "fk_post_prod_job_info_id"))
    @Hidden // FIXME temporary hack : swagger generate a wrong specification, for now avoid to use it
    private JobInfo lastPostProductionJobInfo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() { // NOSONAR
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (productName == null ? 0 : productName.hashCode()); // NOSONAR
        return result;
    }

    @Override
    public boolean equals(Object obj) { // NOSONAR
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Product other = (Product) obj;
        if (productName == null) {
            if (other.productName != null) {
                return false;
            }
        } else if (!productName.equals(other.productName)) {
            return false;
        }
        return true;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void addAcquisitionFile(AcquisitionFile acqFile) {
        this.fileList.add(acqFile);
    }

    public void addAcquisitionFiles(Collection<AcquisitionFile> acqFiles) {
        this.fileList.addAll(acqFiles);
    }

    public Set<AcquisitionFile> getAcquisitionFiles() {
        return fileList;
    }

    /**
     * Filter acquisition files to only retrieve active ones (useful for SIP generation)
     *
     * @return active {@link AcquisitionFile} (i.e. in {@link AcquisitionFileState#ACQUIRED} state)
     */
    public Set<AcquisitionFile> getActiveAcquisitionFiles() {
        return fileList.stream()
                       .filter(af -> AcquisitionFileState.ACQUIRED.equals(af.getState()))
                       .collect(Collectors.toSet());
    }

    public void removeAcquisitionFile(AcquisitionFile acqFile) {
        this.fileList.remove(acqFile);
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public SIPDto getSip() {
        return sip;
    }

    public void setSip(SIPDto sip) {
        this.sip = sip;
    }

    public ProductState getState() {
        return state;
    }

    public void setState(ProductState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(id);
        strBuilder.append(" - ");
        strBuilder.append(productName);
        strBuilder.append(" - ");
        strBuilder.append(state);
        strBuilder.append(" - ");
        strBuilder.append(sipState);
        strBuilder.append(" - ");
        strBuilder.append(session);
        return strBuilder.toString();
    }

    public ISipState getSipState() {
        return sipState;
    }

    public void setSipState(ISipState sipState) {
        this.sipState = sipState;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getIpId() {
        return ipId;
    }

    public void setIpId(String ipId) {
        this.ipId = ipId;
    }

    public AcquisitionProcessingChain getProcessingChain() {
        return processingChain;
    }

    public void setProcessingChain(AcquisitionProcessingChain processingChain) {
        this.processingChain = processingChain;
    }

    public JobInfo getLastSIPGenerationJobInfo() {
        return lastSIPGenerationJobInfo;
    }

    public void setLastSIPGenerationJobInfo(JobInfo lastSIPGenerationJobInfo) {
        this.lastSIPGenerationJobInfo = lastSIPGenerationJobInfo;
    }

    public JobInfo getLastPostProductionJobInfo() {
        return lastPostProductionJobInfo;
    }

    public void setLastPostProductionJobInfo(JobInfo lastPostProductionJobInfo) {
        this.lastPostProductionJobInfo = lastPostProductionJobInfo;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}