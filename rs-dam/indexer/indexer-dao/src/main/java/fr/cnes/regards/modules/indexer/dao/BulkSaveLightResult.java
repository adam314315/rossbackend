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
package fr.cnes.regards.modules.indexer.dao;

import java.io.Serial;
import java.io.Serializable;

/**
 * Object permitting to know what IEsRepository.saveBulk() method has really done.
 *
 * @author sbinda
 */
public class BulkSaveLightResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 8648391036878877723L;

    private int savedDocsCount = 0;

    private int inErrorDocsCount = 0;

    public BulkSaveLightResult() {
        super();
    }

    public int getSavedDocsCount() {
        return savedDocsCount;
    }

    public int getInErrorDocsCount() {
        return inErrorDocsCount;
    }

    /**
     * Append another bulk save result
     *
     * @param bulkSaveResult another bulk save result
     * @return this
     */
    public BulkSaveLightResult append(BulkSaveResult bulkSaveResult) {
        if (bulkSaveResult != null) {
            this.savedDocsCount += bulkSaveResult.getSavedDocsCount();
            this.inErrorDocsCount += bulkSaveResult.getInErrorDocsCount();
        }
        return this;
    }
}
