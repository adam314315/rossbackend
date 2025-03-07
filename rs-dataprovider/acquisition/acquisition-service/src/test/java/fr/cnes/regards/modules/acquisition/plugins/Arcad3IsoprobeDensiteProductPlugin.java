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

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compute DA_TC_ARCAD3_ISO_DENSITE product names
 *
 * @author Marc Sordi
 */
@Plugin(id = "Arcad3IsoprobeDensiteProductPlugin",
        version = "1.0.0-SNAPSHOT",
        description = "Compute the product name from data and browse filenames",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class Arcad3IsoprobeDensiteProductPlugin implements IProductPlugin {

    private static final String BASE_PRODUCT_NAME = "ISO_DENS_";

    private static final Pattern BROWSE_PATTERN = Pattern.compile("iso_nete_([0-9]{8}_[0-9]{4})[BC].png");

    @Override
    public String getProductName(Path filePath) {
        String productName = filePath.getFileName().toString();

        // Retrieve product name
        Matcher m = BROWSE_PATTERN.matcher(productName);
        if (m.matches()) {
            productName = BASE_PRODUCT_NAME + m.group(1);
        }
        return productName;
    }

}
