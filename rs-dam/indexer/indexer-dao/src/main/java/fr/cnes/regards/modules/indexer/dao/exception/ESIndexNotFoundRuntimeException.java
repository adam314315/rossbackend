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
package fr.cnes.regards.modules.indexer.dao.exception;

import fr.cnes.regards.framework.utils.RsRuntimeException;

/**
 * @author Thibaud Michaudel
 **/
public class ESIndexNotFoundRuntimeException extends RsRuntimeException {

    /**
     * Related message error
     */
    private static final String INDEX_NOT_FOUND_ERROR_MESSAGE = "Research won't work until you've ingested some features into ES";

    public ESIndexNotFoundRuntimeException() {
        super(INDEX_NOT_FOUND_ERROR_MESSAGE);
    }
}
