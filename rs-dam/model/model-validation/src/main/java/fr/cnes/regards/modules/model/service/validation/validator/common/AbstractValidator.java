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
package fr.cnes.regards.modules.model.service.validation.validator.common;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Validator for properties and object validation
 *
 * @author Thibaud Michaudel
 **/
public abstract class AbstractValidator implements Validator {

    /**
     * Attribute key
     */
    protected final String attributeKey;

    public AbstractValidator(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    protected void rejectUnsupported(Errors errors) {
        errors.reject("error.unsupported.property.type.message",
                      String.format("Unsupported property \"%s\" for validator \"%s\".",
                                    attributeKey,
                                    this.getClass().getName()));
    }
}
