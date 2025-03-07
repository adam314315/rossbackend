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
package fr.cnes.regards.modules.acquisition.service.plugin;

import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPDtoBuilder;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;

import java.util.UUID;

/**
 * Default SIP generation
 *
 * @author Marc Sordi
 */
@Plugin(id = "LongLastingSIPGeneration",
        version = "1.0.0-SNAPSHOT",
        description = "Generate SIP using product information",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class LongLastingSIPGeneration implements ISipGenerationPlugin {

    //    private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

    @Override
    public SIPDto generate(Product product) throws ModuleException {

        // Wait 5 seconds before SIP generation (DO NOT USE THREAD SLEEP, it will clean interrupted status)
        long startWaitingDate = System.currentTimeMillis();
        long endWaitingDate = startWaitingDate + 5_000;
        while (System.currentTimeMillis() < endWaitingDate) {
        }

        // Init the builder
        SIPDtoBuilder sipBuilder = new SIPDtoBuilder(product.getProductName());

        // Fill SIP with product information
        for (AcquisitionFile af : product.getActiveAcquisitionFiles()) {
            sipBuilder.getContentInformationBuilder()
                      .setDataObject(af.getFileInfo().getDataType(),
                                     af.getFilePath().toAbsolutePath(),
                                     AcquisitionProcessingChain.CHECKSUM_ALGORITHM,
                                     UUID.randomUUID().toString());
            sipBuilder.getContentInformationBuilder().setSyntax(af.getFileInfo().getMimeType());
            sipBuilder.addContentInformation();
        }

        // Add creation event
        sipBuilder.addEvent("Product SIP generation");

        return sipBuilder.build();
    }

}
