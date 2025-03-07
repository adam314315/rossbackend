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

import java.util.ArrayList;
import java.util.List;

public class AcquisitionProcessingChainMonitor {

    private AcquisitionProcessingChain chain;

    @SuppressWarnings("unused")
    private final Long chainId;

    private boolean active = false;

    private boolean deletionPending = false;

    private final List<String> executionBlockers = new ArrayList<>();

    // Post processing jobs not managed here ... can be seen in product

    public AcquisitionProcessingChainMonitor(AcquisitionProcessingChain chain, boolean deletionPending) {
        super();
        this.chain = chain;
        this.chainId = chain.getId();
        this.deletionPending = deletionPending;
    }

    public AcquisitionProcessingChain getChain() {
        return chain;
    }

    public void setChain(AcquisitionProcessingChain chain) {
        this.chain = chain;
    }

    public void setActive(boolean isProductAcquisitionJobActive, long nbSIPGenerationJobs) {
        active = isProductAcquisitionJobActive || (nbSIPGenerationJobs > 0);
    }

    public boolean isActive() {
        return active;
    }

    public boolean isDeletionPending() {
        return deletionPending;
    }

    public void setDeletionPending(boolean deletionPending) {
        this.deletionPending = deletionPending;
    }

    public List<String> getExecutionBlockers() {
        return executionBlockers;
    }

}
