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
package fr.cnes.regards.modules.acquisition.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * @author Christophe Mertz
 */
public class DomainModelException extends ModuleException {

    /**
     *
     */
    private static final long serialVersionUID = 4286494225131649395L;

    /**
     * Constructor
     *
     * @param pException l'exception a propager
     */
    public DomainModelException(Throwable pException) {
        super(pException);
    }

    /**
     * Constructor
     *
     * @param pMessage   le message de l'erreur
     * @param pException l'exception à propager
     */
    public DomainModelException(String pMessage, Throwable pException) {
        super(pMessage, pException);
    }

    /**
     * Constructor
     *
     * @param pMessage le message de l'erreur
     */
    public DomainModelException(String pMessage) {
        super(pMessage);
    }

}
