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
package fr.cnes.regards.framework.modules.jobs.domain;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobWorkspaceException;
import fr.cnes.regards.framework.modules.jobs.domain.function.CheckedSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Map;
import java.util.Observable;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstract job, all jobs must inherit this class
 *
 * @param <R> result type
 * @author oroussel
 * @author Léo Mieulet
 */
public abstract class AbstractJob<R> extends Observable implements IJob<R> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static final String INFO_TAB = " >>>>> ";

    protected UUID jobInfoId;

    protected R result;

    /**
     * The workspace can be null, it should be cleaned after termination of a job
     */
    private Path workspace;

    /**
     * Current completion count
     */
    private int completion = 0;

    /**
     * Set the result if necessary
     *
     * @param result the result
     */
    protected void setResult(R result) {
        this.result = result;
    }

    @Override
    public R getResult() {
        return result;
    }

    @Override
    public void setWorkspace(CheckedSupplier<Path, IOException> workspaceSupplier) throws JobWorkspaceException {
        try {
            workspace = workspaceSupplier.get();
        } catch (IOException e) {
            handleWorkspaceException(e);
        }
    }

    @Override
    public Path getWorkspace() {
        return workspace;
    }

    @Override
    public void advanceCompletion() {
        this.completion++;
        super.setChanged();
        super.notifyObservers(this.completion * 100 / getCompletionCount());
    }

    /**
     * Reject a job because workspace has thrown an IOException
     *
     * @param e thrown exception while setting workspace
     */
    protected void handleWorkspaceException(IOException e) throws JobWorkspaceException {
        logger.error("Cannot set workspace", e);
        throw new JobWorkspaceException(e);
    }

    /**
     * Reject a job because <b>a parameter is missing</b>
     *
     * @param parameterName missing parameter name
     * @throws JobParameterMissingException the related exception
     */
    protected void handleMissingParameter(String parameterName) throws JobParameterMissingException {
        IJob.handleMissingParameter(parameterName);
    }

    /**
     * Reject a job because <b>a parameter is invalid</b>
     *
     * @param parameterName related parameter
     * @param reason        reason for invalidity
     * @throws JobParameterInvalidException the related exception
     */
    protected void handleInvalidParameter(String parameterName, String reason) throws JobParameterInvalidException {
        IJob.handleInvalidParameter(parameterName, reason);
    }

    /**
     * Reject a job because <b>a parameter is invalid</b>
     *
     * @param parameterName related parameter
     * @param reason        reason for invalidity
     * @throws JobParameterInvalidException the related exception
     */
    protected void handleInvalidParameter(String parameterName, Exception reason) throws JobParameterInvalidException {
        IJob.handleInvalidParameter(parameterName, reason);
    }

    /**
     * Get a required non null parameter value
     *
     * @param parameters    map of parameters
     * @param parameterName parameter name to retrieve
     * @param type          to return (may be guessed for simple type, use {@link TypeToken#getType()} instead)
     * @return the parameter value
     * @throws JobParameterMissingException if parameter does not exist
     * @throws JobParameterInvalidException if parameter value is null
     */
    protected <T> T getValue(Map<String, JobParameter> parameters, String parameterName, Type type)
        throws JobParameterMissingException, JobParameterInvalidException {
        return IJob.getValue(parameters, parameterName, type);
    }

    protected <T> T getValue(Map<String, JobParameter> parameters, String parameterName)
        throws JobParameterMissingException, JobParameterInvalidException {
        return IJob.getValue(parameters, parameterName, null);
    }

    /**
     * Get parameter value as an Optional
     *
     * @param parameters    map of parameters
     * @param parameterName parameter name to retrieve
     * @param type          to return (may be guessed for simple type, use {@link TypeToken#getType()} instead)
     * @return an {@link java.util.Optional} parameter value
     */
    protected <T> Optional<T> getOptionalValue(Map<String, JobParameter> parameters, String parameterName, Type type) {
        return IJob.getOptionalValue(parameters, parameterName, type);
    }

    protected <T> Optional<T> getOptionalValue(Map<String, JobParameter> parameters, String parameterName) {
        return IJob.getOptionalValue(parameters, parameterName, null);
    }

    protected UUID getJobInfoId() {
        return jobInfoId;
    }

    @Override
    public void setJobInfoId(UUID jobInfoId) {
        this.jobInfoId = jobInfoId;
    }

}
