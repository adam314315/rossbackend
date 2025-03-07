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
package fr.cnes.regards.modules.acquisition.service.plugins;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPDtoBuilder;
import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.framework.geojson.FeatureCollection;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This plugin allows to scan a directory to find geojson files and generate a new file to acquire for each feature found in it.
 *
 * @author Sébastien Binda
 */
@Plugin(id = "GeoJsonFeatureCollectionParserPlugin",
        version = "1.0.0-SNAPSHOT",
        description = "Scan directory to detect geosjson files. Generate a file to acquire for each feature found in it.",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class GeoJsonFeatureCollectionParserPlugin implements IScanPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobDiskScanning.class);

    public static final String FIELD_FEATURE_ID = "featureId";

    public static final String ALLOW_EMPTY_FEATURES = "allowEmptyFeatures";

    @Autowired
    private Gson gson;

    @PluginParameter(name = FIELD_FEATURE_ID,
                     label = "Json path to access the identifier of each feature in the geojson file",
                     optional = false)
    private String featureId;

    @PluginParameter(name = ALLOW_EMPTY_FEATURES,
                     label = "Generate features with no files (raw, thumbnail & description) associated.",
                     optional = true,
                     defaultValue = "false")
    private boolean allowEmptyFeature;

    @Override
    public List<Path> scan(Path dirPath, Optional<OffsetDateTime> scanningDate) {
        List<Path> scannedFiles = new ArrayList<>();
        if (Files.isDirectory(dirPath)) {
            scannedFiles.addAll(scanDirectory(dirPath, scanningDate));
        } else {
            throw new PluginUtilsRuntimeException(String.format("Invalid directory path : %s", dirPath.toString()));
        }

        return scannedFiles;

    }

    private List<Path> scanDirectory(Path dirPath, Optional<OffsetDateTime> scanningDate) {
        List<Path> genetateFeatureFiles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.geojson")) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    if (scanningDate.isPresent()) {
                        OffsetDateTime lmd = OffsetDateTime.ofInstant(Files.getLastModifiedTime(entry).toInstant(),
                                                                      ZoneOffset.UTC);
                        if (lmd.isAfter(scanningDate.get()) || lmd.isEqual(scanningDate.get())) {
                            genetateFeatureFiles.addAll(generateFeatureFiles(entry));
                        }
                    } else {
                        genetateFeatureFiles.addAll(generateFeatureFiles(entry));
                    }
                }
            }
        } catch (IOException x) {
            throw new PluginUtilsRuntimeException("Scanning failure", x);
        }

        return genetateFeatureFiles;
    }

    private List<Path> generateFeatureFiles(Path entry) {
        List<Path> generatedFiles = new ArrayList<>();

        try {

            // Check if file is a gson feature collection
            File gsonFile = entry.toFile();
            JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(gsonFile)));
            FeatureCollection fc = gson.fromJson(reader, FeatureCollection.class);

            for (Feature feature : fc.getFeatures()) {
                String name = (String) feature.getProperties().get(featureId);
                SIPDtoBuilder builder = new SIPDtoBuilder(name);
                for (String property : feature.getProperties().keySet()) {
                    Object value = feature.getProperties().get(property);
                    if (value != null) {
                        builder.addDescriptiveInformation(property, value);
                    }
                }

                // Check for RAWDATA if any
                Path rawDataFile = Paths.get(entry.getParent().toString(), name + ".dat");
                Path thumbnailFilePng = Paths.get(entry.getParent().toString(), name + ".png");
                Path thumbnailFileJpg = Paths.get(entry.getParent().toString(), name + ".jpg");
                Path descFile = Paths.get(entry.getParent().toString(), name + ".pdf");

                if (Files.exists(rawDataFile)) {
                    String checksum = ChecksumUtils.computeHexChecksum(new FileInputStream(rawDataFile.toFile()),
                                                                       "MD5");
                    builder.getContentInformationBuilder()
                           .setDataObject(DataType.RAWDATA,
                                          rawDataFile.toAbsolutePath(),
                                          rawDataFile.getFileName().toString(),
                                          "MD5",
                                          checksum,
                                          rawDataFile.toFile().length());
                    builder.getContentInformationBuilder().setSyntax(MediaType.APPLICATION_OCTET_STREAM);
                    builder.addContentInformation();
                }
                if (Files.exists(thumbnailFilePng)) {
                    String checksum = ChecksumUtils.computeHexChecksum(new FileInputStream(thumbnailFilePng.toFile()),
                                                                       "MD5");
                    builder.getContentInformationBuilder()
                           .setDataObject(DataType.THUMBNAIL,
                                          thumbnailFilePng.toAbsolutePath(),
                                          thumbnailFilePng.getFileName().toString(),
                                          "MD5",
                                          checksum,
                                          thumbnailFilePng.toFile().length());
                    builder.getContentInformationBuilder().setSyntax(MediaType.IMAGE_PNG);
                    builder.addContentInformation();

                    builder.getContentInformationBuilder()
                           .setDataObject(DataType.QUICKLOOK_SD,
                                          thumbnailFilePng.toAbsolutePath(),
                                          thumbnailFilePng.getFileName().toString(),
                                          "MD5",
                                          checksum,
                                          thumbnailFilePng.toFile().length());
                    builder.getContentInformationBuilder().setSyntax(MediaType.IMAGE_PNG);
                    builder.addContentInformation();
                }
                if (Files.exists(thumbnailFileJpg)) {
                    String checksum = ChecksumUtils.computeHexChecksum(new FileInputStream(thumbnailFileJpg.toFile()),
                                                                       "MD5");
                    builder.getContentInformationBuilder()
                           .setDataObject(DataType.THUMBNAIL,
                                          thumbnailFileJpg.toAbsolutePath(),
                                          thumbnailFileJpg.getFileName().toString(),
                                          "MD5",
                                          checksum,
                                          thumbnailFileJpg.toFile().length());
                    builder.getContentInformationBuilder().setSyntax(MediaType.IMAGE_PNG);
                    builder.addContentInformation();

                    builder.getContentInformationBuilder()
                           .setDataObject(DataType.QUICKLOOK_SD,
                                          thumbnailFileJpg.toAbsolutePath(),
                                          thumbnailFileJpg.getFileName().toString(),
                                          "MD5",
                                          checksum,
                                          thumbnailFileJpg.toFile().length());
                    builder.getContentInformationBuilder().setSyntax(MediaType.IMAGE_PNG);
                    builder.addContentInformation();
                }
                if (Files.exists(descFile)) {
                    String checksum = ChecksumUtils.computeHexChecksum(new FileInputStream(descFile.toFile()), "MD5");
                    builder.getContentInformationBuilder()
                           .setDataObject(DataType.DESCRIPTION,
                                          descFile.toAbsolutePath(),
                                          descFile.getFileName().toString(),
                                          "MD5",
                                          checksum,
                                          descFile.toFile().length());
                    builder.getContentInformationBuilder().setSyntax(MediaType.APPLICATION_PDF);
                    builder.addContentInformation();
                }

                SIPDto sip = builder.build();
                sip.setGeometry(feature.getGeometry());

                if (!sip.getProperties().getContentInformations().isEmpty() || allowEmptyFeature) {
                    Path file = Paths.get(entry.getParent().toString(), name + ".json");
                    generatedFiles.add(Files.write(file, Arrays.asList(gson.toJson(sip)), Charset.forName("UTF-8")));
                }
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return generatedFiles;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

}
