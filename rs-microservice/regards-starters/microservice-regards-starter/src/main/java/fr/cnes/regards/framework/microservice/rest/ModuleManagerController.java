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
package fr.cnes.regards.framework.microservice.rest;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.gson.GsonBuilderFactory;
import fr.cnes.regards.framework.gson.adapters.ClassAdapter;
import fr.cnes.regards.framework.gson.strategy.SerializationExclusionStrategy;
import fr.cnes.regards.framework.microservice.manager.MicroserviceConfiguration;
import fr.cnes.regards.framework.module.manager.*;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Module manager controller
 *
 * @author Marc Sordi
 */
@RestController
@RequestMapping(ModuleManagerController.TYPE_MAPPING)
public class ModuleManagerController {

    public static final String TYPE_MAPPING = "/microservice";

    public static final String CONFIGURATION_MAPPING = "/configuration";

    public static final String READY_MAPPING = "/ready";

    public static final String RESTART_MAPPING = "/restart";

    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleManagerController.class);

    private static final String ENABLED_MAPPING = "/enabled";

    public static final String CONFIGURATION_ENABLED_MAPPING = CONFIGURATION_MAPPING + ENABLED_MAPPING;

    public static final String READY_ENABLED_MAPPING = READY_MAPPING + ENABLED_MAPPING;

    public static final String RESTART_ENABLED_MAPPING = RESTART_MAPPING + ENABLED_MAPPING;

    /**
     * Prefix for imported/exported filename
     */
    private static final String CONFIGURATION_FILE_PREFIX = "config-";

    /**
     * Suffix for imported/exported filename
     */
    private static final String CONFIGURATION_FILE_EXTENSION = ".json";

    @Value("${spring.application.name}")
    private String microserviceName;

    @Autowired
    private GsonBuilderFactory gsonBuilderFactory;

    private Gson configGson;

    @Autowired(required = false)
    private List<IModuleManager<?>> managers;

    @GetMapping(value = CONFIGURATION_ENABLED_MAPPING)
    @ResourceAccess(description = "Import/export support information", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> isConfigurationEnabled() {
        if ((managers != null) && !managers.isEmpty()) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @GetMapping(value = CONFIGURATION_MAPPING)
    @ResourceAccess(description = "Export microservice configuration", role = DefaultRole.PROJECT_ADMIN)
    public void exportConfiguration(HttpServletRequest request, HttpServletResponse response) throws ModuleException {

        String exportedFilename = CONFIGURATION_FILE_PREFIX + microserviceName + CONFIGURATION_FILE_EXTENSION;

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportedFilename + "\"");

        // Prepare data
        MicroserviceConfiguration microConfig = new MicroserviceConfiguration();
        microConfig.setMicroservice(microserviceName);
        if ((managers != null) && !managers.isEmpty()) {
            for (IModuleManager<?> manager : managers) {
                microConfig.addModule(manager.exportConfiguration());
            }
        }

        // Stream data
        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(response.getOutputStream(),
                                                                       StandardCharsets.UTF_8))) {
            writer.setIndent("  ");
            configGson.toJson(microConfig, MicroserviceConfiguration.class, writer);
            writer.flush();
        } catch (IOException e) {
            String message = String.format("Error exporting configuration for microservice %s", microserviceName);
            LOGGER.error(message, e);
            throw new ModuleException(e);
        }
    }

    @PostMapping(value = CONFIGURATION_MAPPING, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResourceAccess(description = "Import microservice configuration", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Set<ModuleImportReport>> importConfiguration(@RequestParam("file") MultipartFile file)
        throws ModuleException {

        try (JsonReader reader = new JsonReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            MicroserviceConfiguration microConfig = configGson.fromJson(reader, MicroserviceConfiguration.class);
            // Propagate configuration to modules
            if ((managers != null) && !managers.isEmpty()) {
                Set<ModuleImportReport> importReports = importConfiguration(microConfig);
                Set<ModuleImportReport> modulesInError = getModulesInError(importReports);
                return makeImportResponse(importReports, modulesInError);
            } else {
                LOGGER.warn("Configuration cannot be imported because no module configuration manager is found!");
                return ResponseEntity.unprocessableEntity().build();
            }

        } catch (IOException | IllegalArgumentException | JsonParseException e) {
            String message = String.format("Error importing configuration for microservice %s", microserviceName);
            LOGGER.error(message, e);
            throw new ModuleException(e);
        }
    }

    private Set<ModuleImportReport> getModulesInError(Set<ModuleImportReport> importReports) {
        return importReports.stream()
                            .filter(mir -> mir.isOnlyErrors() || !mir.getImportErrors().isEmpty())
                            .collect(Collectors.toSet());
    }

    private ResponseEntity<Set<ModuleImportReport>> makeImportResponse(Set<ModuleImportReport> importReports,
                                                                       Set<ModuleImportReport> modulesInError) {
        // if there is no error at all
        if (modulesInError.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            //if not all import had errors
            if (modulesInError.size() < importReports.size()) {
                return new ResponseEntity<>(importReports, HttpStatus.PARTIAL_CONTENT);
            } else {
                // now that we know that every module has errors, lets check if any configuration at all could be imported
                long numberModulesInTotalError = modulesInError.stream()
                                                               .filter(ModuleImportReport::isOnlyErrors)
                                                               .count();
                if (numberModulesInTotalError == modulesInError.size()) {
                    return new ResponseEntity<>(importReports, HttpStatus.CONFLICT);
                } else {
                    return new ResponseEntity<>(importReports, HttpStatus.PARTIAL_CONTENT);
                }
            }
        }
    }

    private Set<ModuleImportReport> importConfiguration(MicroserviceConfiguration microConfig) {
        Set<ModuleImportReport> importReports = new HashSet<>();
        for (ModuleConfiguration module : microConfig.getModules()) {
            for (IModuleManager<?> manager : managers) {
                if (manager.isApplicable(module)) {
                    Set<String> resetErrors = Sets.newHashSet();
                    if (module.isResetBeforeImport()) {
                        resetErrors = manager.resetConfiguration();
                    }
                    ModuleImportReport report = manager.importConfigurationAndLog(module);
                    report.getImportErrors().addAll(resetErrors);
                    importReports.add(report);
                }
            }
        }
        return importReports;
    }

    @EventListener
    public void onApplicationStartedEvent(ApplicationStartedEvent applicationStartedEvent) {
        // Create GSON for generic module configuration item adapter without itself! (avoid stackOverflow)
        GsonBuilder customBuilder = gsonBuilderFactory.newBuilder();
        customBuilder.addSerializationExclusionStrategy(new SerializationExclusionStrategy<>(ConfigIgnore.class));
        customBuilder.registerTypeHierarchyAdapter(Class.class, new ClassAdapter());
        Gson configItemGson = customBuilder.create();

        // Create GSON with specific adapter to dynamically analyze parameterized type
        customBuilder = gsonBuilderFactory.newBuilder();
        customBuilder.addSerializationExclusionStrategy(new SerializationExclusionStrategy<>(ConfigIgnore.class));
        customBuilder.registerTypeHierarchyAdapter(Class.class, new ClassAdapter());
        customBuilder.registerTypeHierarchyAdapter(ModuleConfigurationItem.class,
                                                   new ModuleConfigurationItemAdapter(configItemGson));
        configGson = customBuilder.create();
    }

    /**
     * @return whether the microservice is ready or not with the reasons
     */
    @GetMapping(value = READY_MAPPING)
    @ResourceAccess(description = "allows to known if the microservice is ready to work",
                    role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<ModuleReadinessReport<?>> isReady() {
        ModuleReadinessReport<Object> microserviceReadiness = new ModuleReadinessReport<>(Boolean.TRUE,
                                                                                          Lists.newArrayList(),
                                                                                          null);
        if ((managers != null) && !managers.isEmpty()) {
            for (IModuleManager<?> manager : managers) {
                if (manager.isReadyImplemented()) {
                    ModuleReadinessReport<?> moduleReadiness = manager.isReady();
                    microserviceReadiness.setReady(microserviceReadiness.isReady() && moduleReadiness.isReady());
                    microserviceReadiness.setSpecifications(moduleReadiness.getSpecifications());
                    if (moduleReadiness.getReasons() != null) {
                        microserviceReadiness.getReasons().addAll(moduleReadiness.getReasons());
                    }
                }
            }
        }
        return new ResponseEntity<>(microserviceReadiness, HttpStatus.OK);
    }

    @GetMapping(value = READY_ENABLED_MAPPING)
    @ResourceAccess(description = "Check if microservice modules ready feature is enabled",
                    role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> isReadyEnabled() {
        if ((managers != null) && !managers.isEmpty()) {
            for (IModuleManager<?> manager : managers) {
                if (manager.isReadyImplemented()) {
                    // At least one module can be asked for "readyness"
                    return ResponseEntity.ok().build();
                }
            }
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    /**
     * Restart all or part of microservice modules
     */
    @GetMapping(value = RESTART_MAPPING)
    @ResourceAccess(description = "Allows to restart all microservice modules", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Set<ModuleRestartReport>> restart() {
        Set<ModuleRestartReport> reports = new HashSet<>();
        if ((managers != null) && !managers.isEmpty()) {
            for (IModuleManager<?> manager : managers) {
                if (manager.isRestartImplemented()) {
                    reports.add(manager.restart());
                }
            }
        }
        return new ResponseEntity<>(reports, HttpStatus.OK);
    }

    @GetMapping(value = RESTART_ENABLED_MAPPING)
    @ResourceAccess(description = "Check if microservice modules restart is enabled", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> isRestartEnabled() {
        if ((managers != null) && !managers.isEmpty()) {
            for (IModuleManager<?> manager : managers) {
                if (manager.isRestartImplemented()) {
                    // At least one module can be restarted
                    return ResponseEntity.ok().build();
                }
            }
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
}
