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
package fr.cnes.regards.modules.acquisition.service.plugins;

import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin test
 *
 * @author Marc Sordi
 */
public class AcquisitionPluginTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionPluginTest.class);

    private static final String MODULE_PACKAGE = "fr.cnes.regards.modules.acquisition";

    // private final Gson gson = new Gson();

    @Test
    public void buildMetadata() {
        PluginUtils.setup(MODULE_PACKAGE);
        PluginMetaData mtd = PluginUtils.createPluginMetaData(GlobDiskScanning.class);
        Assert.assertNotNull(mtd);
        // LOGGER.info(gson.toJson(mtd));
    }
}
