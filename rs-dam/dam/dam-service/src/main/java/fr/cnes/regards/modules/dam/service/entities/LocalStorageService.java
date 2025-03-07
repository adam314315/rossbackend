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
package fr.cnes.regards.modules.dam.service.entities;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.dam.dao.entities.ILocalFileRepository;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.LocalFile;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * A service to save document files on disk.</br>
 *
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class LocalStorageService implements ILocalStorageService, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalStorageService.class);

    public static final String DIGEST_ALGORITHM = "MD5";

    /**
     * This constant is used by the controller to create an URL that contains a flag that will be replaced by
     * the real checksum here in the service
     */
    public static final String FILE_CHECKSUM_URL_TEMPLATE = "~checksumFlag~";

    @Autowired
    private ILocalFileRepository localStorageRepo;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Folder where this service will store all document files
     */
    @Value("${regards.dam.local_storage.path:/tmp/rs-dam-ls}")
    private String localStoragePath;

    @Override
    public void afterPropertiesSet() {
        // Init localStoragePath folder if necessary
        Path path = Paths.get(this.localStoragePath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                LOGGER.error("Could not create the localStoragePath directory (" + localStoragePath + ")", e);
                throw new RsRuntimeException(e);
            }
        }
    }

    @Override
    public Collection<DataFile> attachFiles(AbstractEntity<?> entity,
                                            DataType dataType,
                                            MultipartFile[] attachments,
                                            String fileUriTemplate) throws ModuleException {
        Set<DataFile> docFiles = new HashSet<>();
        if (attachments != null) {
            try {
                for (MultipartFile file : attachments) {
                    if ((file != null) && !file.isEmpty()) {
                        ContentTypeValidator.supports(dataType, file.getOriginalFilename(), file.getContentType());
                        String checksum = ChecksumUtils.computeHexChecksum(file.getInputStream(), DIGEST_ALGORITHM);
                        URI fileRef = new URI(fileUriTemplate.replace(FILE_CHECKSUM_URL_TEMPLATE, checksum));

                        // Build data file
                        DataFile dataFile = DataFile.build(dataType,
                                                           file.getOriginalFilename(),
                                                           fileRef,
                                                           MimeType.valueOf(file.getContentType()),
                                                           Boolean.TRUE,
                                                           Boolean.FALSE);
                        dataFile.setFilesize(file.getSize());
                        dataFile.setDigestAlgorithm(DIGEST_ALGORITHM);
                        dataFile.setChecksum(checksum);
                        dataFile.setFilename(file.getOriginalFilename());
                        store(checksum, file.getOriginalFilename(), file.getInputStream(), entity);
                        docFiles.add(dataFile);
                    }
                }
            } catch (URISyntaxException | IOException e) {
                String message = "Error during attaching file";
                LOGGER.error(message, e);
                throw new ModuleException(message, e);
            } catch (NoSuchAlgorithmException e) {
                String message = "No such algorithm (used on checksum computation)";
                LOGGER.error(message, e);
                throw new ModuleException(message, e);
            }
        }
        return docFiles;
    }

    /**
     * This methods tries to delete a local file attached to the document if existing
     * But does not raise any error if the file does not exist on rs-dam
     */
    @Override
    public void removeFile(AbstractEntity<?> entity, DataFile dataFile) throws ModuleException {

        // Retrieve reference
        Optional<LocalFile> localFileOpt = localStorageRepo.findOneByEntityAndFileChecksum(entity,
                                                                                           dataFile.getChecksum());
        if (localFileOpt.isEmpty()) {
            throw new EntityNotFoundException(String.format("Failed to remove the file %s for the document %s",
                                                            dataFile.getFilename(),
                                                            entity.getIpId()), LocalFile.class);
        }

        Path filePath = getDataFilePath(dataFile.getChecksum());
        if (Files.exists(filePath)) {
            // Check rights
            if (!Files.isWritable(filePath)) {
                throw new ModuleException(String.format("Failed to remove the file %s : Permission refused",
                                                        filePath.toString()));
            }
            // Remove file
            // Do not remove the file if its used by another document
            Long countFiles = localStorageRepo.countByFileChecksum(dataFile.getChecksum());
            if (countFiles == 1) {
                try {
                    Files.delete(filePath);
                } catch (IOException e) {
                    String message = String.format("Cannot delete file %s", filePath.toAbsolutePath());
                    LOGGER.error(message, e);
                    throw new ModuleException(message);
                }
            } else {
                LOGGER.info("File %s was not removed on disk since another document uses it", filePath);
            }
        }
        // Remove from database
        localStorageRepo.deleteById(localFileOpt.get().getId());

    }

    /**
     * Check if the database knows the dataFile
     */
    @Override
    public boolean isFileLocallyStored(AbstractEntity<?> entity, DataFile dataFile) {
        Optional<LocalFile> localFile = localStorageRepo.findOneByEntityAndFileChecksum(entity, dataFile.getChecksum());
        return localFile.isPresent();
    }

    @Override
    public void getFileContent(String checksum, OutputStream output) throws ModuleException {
        Path filePath = getDataFilePath(checksum);
        try {
            Files.copy(filePath, output);
        } catch (IOException e) {
            String message = String.format("Cannot stream file %s", filePath.toAbsolutePath());
            LOGGER.error(message, e);
            throw new ModuleException(message);
        }

    }

    /**
     * Store file in local storage and keep a database reference
     */
    private void store(String checksum, String filename, InputStream inputStream, AbstractEntity<?> entity)
        throws IOException {
        if (localStorageRepo.findOneByEntityAndFileChecksum(entity, checksum).isPresent()) {
            // Silently skip
            LOGGER.warn("File {} already attached to the entity {}. Skipping store action.",
                        filename,
                        entity.getLabel());
            return;
        }

        if (localStorageRepo.countByFileChecksum(checksum) == 0) {
            // Copy file

            // We assume that baseStorageLocation already exists on the file system.
            // We just need to create, if required, the directory between localStoragePath and the file.

            Path folderPath = getDataFolder(checksum);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            // Save file
            Path filePath = getDataFilePath(checksum);
            // Accept a file to be attached to several entities
            if (!Files.exists(filePath)) {
                Files.copy(inputStream, filePath);
            }
        }

        // Save reference in database
        LocalFile localStorage = LocalFile.build(entity, checksum);
        localStorageRepo.save(localStorage);
    }

    @Override
    public Collection<DataFile> attachLocalFiles(AbstractEntity<?> entity,
                                                 DataType dataType,
                                                 List<DataFile> localFiles,
                                                 String fileUriTemplate) throws ModuleException {
        Set<DataFile> docFiles = new HashSet<>();
        try {
            for (DataFile dataFile : localFiles) {
                File file = new File(dataFile.asUri());
                if (file.exists()) {
                    ContentTypeValidator.supports(dataType, file.getName(), "");
                    // Build data file
                    dataFile.setReference(Boolean.FALSE);
                    // update file uri
                    dataFile.setFilesize(Files.size(file.toPath()));
                    dataFile.setDigestAlgorithm(DIGEST_ALGORITHM);
                    dataFile.setChecksum(ChecksumUtils.computeHexChecksum(file.toPath(), DIGEST_ALGORITHM));
                    docFiles.add(dataFile);
                    store(dataFile.getChecksum(), dataFile.getFilename(), new FileInputStream(file), entity);
                } else {
                    throw new ModuleException(String.format("File at location %s not exists", dataFile.asUri()));
                }
            }
        } catch (IOException e) {
            String message = "Error during attaching file";
            LOGGER.error(message, e);
            throw new ModuleException(message, e);
        } catch (NoSuchAlgorithmException e) {
            String message = "No such algorithm (used on checksum computation)";
            LOGGER.error(message, e);
            throw new ModuleException(message, e);
        }
        return docFiles;
    }

    /**
     * The DataFile path is the DataFileFolder + checksum
     */
    private Path getDataFilePath(String checksum) {
        return getDataFolder(checksum).resolve(checksum);
    }

    /**
     * Lets compute the file folder localStoragePath + tenant + 2 first char of checksum
     */
    private Path getDataFolder(String checksum) {
        return Paths.get(localStoragePath, runtimeTenantResolver.getTenant(), checksum.substring(0, 2));
    }
}
