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
package fr.cnes.regards.modules.acquisition.plugins;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

import java.nio.file.Path;

/**
 * Second <b>optional</b> step of acquisition processing chain. This step is used to validate a file.
 *
 * @author Marc Sordi
 */
@PluginInterface(description = "File validation plugin contract")
public interface IValidationPlugin {

    /**
     * Validate a file
     *
     * @param filePath file to validate
     * @return true if file is valid
     * @throws ModuleException if error occurs!
     */
    boolean validate(Path filePath) throws ModuleException;
}
