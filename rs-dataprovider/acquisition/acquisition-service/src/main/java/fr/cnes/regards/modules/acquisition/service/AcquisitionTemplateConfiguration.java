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
package fr.cnes.regards.modules.acquisition.service;

import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.service.TemplateConfigUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Initialize notifications message templates for acquisition service.
 *
 * @author sbinda
 */
@Configuration
public class AcquisitionTemplateConfiguration {

    public static final String ACQUISITION_INVALID_FILES_TEMPLATE = "ACQUISITION_INVALID_FILES_TEMPLATE";

    public static final String EXECUTION_BLOCKERS_TEMPLATE = "EXECUTION_BLOCKERS_TEMPLATE";

    @Bean
    public Template acquInvalidFilesTemplate() throws IOException {
        return TemplateConfigUtil.readTemplate(ACQUISITION_INVALID_FILES_TEMPLATE,
                                               "template/acquisition_invalid_files.html");
    }

    @Bean
    public Template executionBlockersTemplate() throws IOException {
        return TemplateConfigUtil.readTemplate(EXECUTION_BLOCKERS_TEMPLATE, "template/execution_blockers.html");
    }

}
