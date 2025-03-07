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
package fr.cnes.regards.modules.ingest.service.job;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.ingest.domain.exception.NothingToDoException;
import fr.cnes.regards.modules.ingest.domain.request.dump.AIPSaveMetadataRequest;
import fr.cnes.regards.modules.ingest.service.dump.IAIPMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * This job is used to dump aips.
 *
 * @author Iliana Ghazali
 * @author Sylvain VISSIERE-GUERINET
 */
public class AIPSaveMetadataJob extends AbstractJob<Void> {

    public static final String SAVE_METADATA_REQUEST = "SAVE_METADATA_REQUEST";

    private AIPSaveMetadataRequest metadataRequest;

    @Autowired
    private IAIPMetadataService metadataService;

    @Override
    public boolean needWorkspace() {
        return true;
    }

    @Autowired
    private IWorkspaceService workspaceService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve param
        Type type = new TypeToken<AIPSaveMetadataRequest>() {

        }.getType();
        this.metadataRequest = getValue(parameters, SAVE_METADATA_REQUEST, type);
    }

    @Override
    public void run() {
        logger.debug("[AIP SAVE METADATA JOB] Running job for 1 AIPSaveMetaDataRequest request");
        long start = System.currentTimeMillis();
        try {
            // Define path for dumpLocation
            String dumpLocationStr = metadataRequest.getDumpLocation();
            Path dumpLocation;
            if (StringUtils.isEmpty(dumpLocationStr)) {
                dumpLocation = workspaceService.getMicroserviceWorkspace();
            } else {
                dumpLocation = Paths.get(dumpLocationStr);
            }
            // Write dump
            metadataService.writeZips(metadataRequest, getWorkspace());
            metadataService.writeDump(metadataRequest, dumpLocation, getWorkspace());
            logger.info("[AIP SAVE METADATA JOB] Dump successfully done between {} {}",
                        metadataRequest.getPreviousDumpDate(),
                        metadataRequest.getCreationDate());
            metadataService.handleSuccess(metadataRequest);
        } catch (IOException e) {
            String errorMessage = e.getClass().getSimpleName() + " " + e.getMessage();
            logger.error(errorMessage, e);
            metadataService.handleError(metadataRequest, errorMessage);
            throw new RsRuntimeException(e);
        } catch (NothingToDoException e) {
            logger.info("[AIP SAVE METADATA JOB] {}", e.getMessage());
            metadataService.handleSuccess(metadataRequest); // request is in success, even if nothing was dumped
        }
        logger.debug("[AIP SAVE META JOB] Job handled for 1 AIPSaveMetaDataRequest request in {}ms",
                     System.currentTimeMillis() - start);
        // there is only one request per job so interruption can be ignored i.e this job(i.e. request) will be fully handled.
    }

}
