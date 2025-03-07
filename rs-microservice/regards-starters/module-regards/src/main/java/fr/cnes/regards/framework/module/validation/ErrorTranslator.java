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
package fr.cnes.regards.framework.module.validation;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Utility class for manipulating validation {@link Errors}
 *
 * @author Marc SORDI
 */
public final class ErrorTranslator {

    private ErrorTranslator() {
    }

    /**
     * Build a set of error string from a not empty {@link Errors} object.
     */
    public static Set<String> getErrors(Errors errors) {
        Assert.isTrue(errors.hasErrors(), "This method must be called only if at least one error exists");
        Set<String> err = new HashSet<>();
        errors.getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                err.add(String.format("[%s] %s at %s: rejected value [%s].",
                                      fieldError.getCode(),
                                      fieldError.getDefaultMessage(),
                                      fieldError.getField(),
                                      ObjectUtils.nullSafeToString(fieldError.getRejectedValue())));
            } else {
                err.add(String.format("[%s] %s", error.getCode(), error.getDefaultMessage()));
            }
        });
        return err;
    }

    /**
     * Build and join a set of error string from a not empty {@link Errors} object
     */
    public static String getErrorsAsString(Errors errors) {
        Set<String> errs = ErrorTranslator.getErrors(errors);
        StringJoiner joiner = new StringJoiner(", ");
        errs.forEach(joiner::add);
        return joiner.toString();
    }
}
