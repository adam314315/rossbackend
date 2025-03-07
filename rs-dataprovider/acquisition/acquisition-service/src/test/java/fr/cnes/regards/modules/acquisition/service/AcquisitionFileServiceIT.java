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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.*;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Test {@link AcquisitionFileService}
 *
 * @author Sébastien Binda
 * @author Marc Sordi
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acq_product" })
@MultitenantTransactional
public class AcquisitionFileServiceIT extends AbstractMultitenantServiceIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductAcquisitionServiceIT.class);

    @Autowired
    private IAcquisitionProcessingService processingService;

    @SuppressWarnings("unused")
    @Autowired
    private IAcquisitionFileRepository acqFileRepository;

    @Autowired
    private IProductService productService;

    @SuppressWarnings("unused")
    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IAcquisitionFileService fileService;

    private AcquisitionProcessingChain processingChain;

    private AcquisitionProcessingChain processingChain2;

    private Product product;

    @Before
    public void init() throws ModuleException {
        createFiles();
    }

    public AcquisitionProcessingChain createProcessingChain(String label) throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel(label);
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");
        processingChain.setPeriodicity("0 * * * * *");
        processingChain.setCategories(org.assertj.core.util.Sets.newLinkedHashSet());

        List<StorageMetadataProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataProvider.build("AWS", "/path/to/file", new HashSet<>()));
        storages.add(StorageMetadataProvider.build("HELLO", "/other/path/to/file", new HashSet<>()));
        processingChain.setStorages(storages);

        // Search directory
        Path searchDir = Paths.get("src", "test", "resources", "data", "plugins", "scan");

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);
        fileInfo.setScanDirInfo(Sets.newHashSet(new ScanDirectoryInfo(searchDir, null)));

        PluginConfiguration scanPlugin = PluginConfiguration.build(GlobDiskScanning.class, null, null);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin - " + label);
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginConfiguration.build(DefaultFileValidation.class,
                                                                         null,
                                                                         new HashSet<IPluginParam>());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin " + label);
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginConfiguration.build(DefaultProductPlugin.class,
                                                                      null,
                                                                      new HashSet<IPluginParam>());
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin" + label);
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginConfiguration.build(DefaultSIPGeneration.class,
                                                                     null,
                                                                     new HashSet<IPluginParam>());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin" + label);
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        // Save processing chain
        return processingService.createChain(processingChain);
    }

    public void createFiles() throws ModuleException {

        processingChain = createProcessingChain("Chaine1");
        processingChain2 = createProcessingChain("Chaine2");
        product = new Product();
        product.setIpId("IpId");
        product.setLastUpdate(OffsetDateTime.now());
        product.setProcessingChain(processingChain);
        product.setProductName("product name");
        product.setSession("session");
        product.setSipState(SIPState.INGESTED);
        product.setState(ProductState.COMPLETED);
        product = productService.save(product);

        // Add acquisition files for chain 1
        int idx = 0;
        for (AcquisitionFileState fileState : AcquisitionFileState.values()) {
            AcquisitionFile file = new AcquisitionFile();
            file.setAcqDate(OffsetDateTime.now());
            file.setError("");
            file.setFileInfo(processingChain.getFileInfos().iterator().next());
            file.setFilePath(Paths.get("/chain2/file" + idx));
            file.setProduct(product);
            file.setState(fileState);
            fileService.save(file);
            idx++;
        }

        // Add acquisition files for chain 2
        for (AcquisitionFileState fileState : AcquisitionFileState.values()) {
            AcquisitionFile file = new AcquisitionFile();
            file.setAcqDate(OffsetDateTime.now());
            file.setError("");
            file.setFileInfo(processingChain2.getFileInfos().iterator().next());
            file.setFilePath(Paths.get("/chain2/file" + idx));
            file.setProduct(product);
            file.setState(fileState);
            fileService.save(file);
            idx++;
        }
    }

    @Test
    public void testCountFilesforProcessingChain() {
        Assert.assertTrue(fileService.countByChain(processingChain) == AcquisitionFileState.values().length);
        for (AcquisitionFileState fileState : AcquisitionFileState.values()) {
            Assert.assertTrue(fileService.countByChainAndStateIn(processingChain, Arrays.asList(fileState)) == 1);
        }
    }

    @Test
    public void testSearchFiles() {
        Page<AcquisitionFile> results = fileService.search("file", null, null, null, null, PageRequest.of(0, 100));
        Assert.assertTrue(results.getNumberOfElements() == (AcquisitionFileState.values().length * 2));

        results = fileService.search("/other", null, null, null, null, PageRequest.of(0, 100));
        Assert.assertTrue(results.getNumberOfElements() == 0);

        results = fileService.search("file1", null, null, processingChain.getId(), null, PageRequest.of(0, 100));
        Assert.assertTrue(results.getNumberOfElements() == 1);

        results = fileService.search(null, null, null, processingChain.getId(), null, PageRequest.of(0, 100));
        Assert.assertTrue(results.getNumberOfElements() == AcquisitionFileState.values().length);

        results = fileService.search(null, null, null, processingChain2.getId(), null, PageRequest.of(0, 100));
        Assert.assertTrue(results.getNumberOfElements() == AcquisitionFileState.values().length);

        results = fileService.search("file",
                                     Arrays.asList(AcquisitionFileState.ACQUIRED),
                                     product.getId(),
                                     processingChain.getId(),
                                     OffsetDateTime.now().minusDays(1),
                                     PageRequest.of(0, 100));
        Assert.assertTrue(results.getNumberOfElements() == 1);
    }

}
