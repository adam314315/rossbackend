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
package fr.cnes.regards.framework.utils.plugins.generics;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import org.junit.Assert;

import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Marc Sordi
 */
@Plugin(author = "REGARDS Team",
        description = "Plugin with String map parameters",
        id = "PluginWithStringMap",
        version = "1.0.0",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CNES",
        url = "https://regardsoss.github.io/")
public class PluginWithStringMap implements IPluginWithGenerics {

    // Attribute name
    public static final String FIELD_NAME = "infos";

    @PluginParameter(keylabel = "Key", label = "String information", description = "Map of infos as string")
    private Map<String, String> infos;

    @Override
    public void doIt() {
        Assert.assertNotNull(infos);
        Assert.assertTrue(infos instanceof Map<?, ?>);
        Assert.assertEquals(3, infos.size());
        for (Entry<String, String> info : infos.entrySet()) {
            Assert.assertTrue(info.getKey() instanceof String);
            Assert.assertTrue(info.getValue() instanceof String);
        }
    }
}
