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
package fr.cnes.regards.modules.filecatalog.client.listener;

import fr.cnes.regards.modules.filecatalog.client.RequestInfo;

import java.util.Set;

/**
 * Listener to implements to handle storage group requests results.
 *
 * @author Sébastien Binda
 */
public interface IStorageRequestListener {

    /**
     * Callback when a group request is granted.
     */
    void onRequestGranted(Set<RequestInfo> requests);

    /**
     * Callback when a group request is denied
     */
    void onRequestDenied(Set<RequestInfo> requests);

    /**
     * Callback when a copy group request is successfully done.
     */
    void onCopySuccess(Set<RequestInfo> requests);

    /**
     * Callback when a copy group request is terminated with errors.
     */
    void onCopyError(Set<RequestInfo> requests);

    /**
     * Callback when a availability group request is successfully done.
     */
    void onAvailable(Set<RequestInfo> requests);

    /**
     * Callback when a availability group request is terminated with errors.
     */
    void onAvailabilityError(Set<RequestInfo> requests);

    /**
     * Callback when a deletion group request is successfully done.
     */
    void onDeletionSuccess(Set<RequestInfo> requests);

    /**
     * Callback when a deletion group request is terminated with errors.
     */
    void onDeletionError(Set<RequestInfo> requests);

    /**
     * Callback when a reference group request is successfully done.
     */
    void onReferenceSuccess(Set<RequestInfo> requests);

    /**
     * Callback when a reference group request is terminated with errors.
     */
    void onReferenceError(Set<RequestInfo> requests);

    /**
     * Callback when a storage group request is successfully done.
     */
    void onStoreSuccess(Set<RequestInfo> requests);

    /**
     * Callback when a storage group request is terminated with errors.
     */
    void onStoreError(Set<RequestInfo> requests);

}
