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
package fr.cnes.regards.framework.utils.plugins.free;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.framework.utils.plugins.generics.PluginWithBoolean;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Free plugin tests
 *
 * @author Marc Sordi
 */
public class PluginFreeTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginFreeTest.class);

    @Test
    public void stringTest() throws NotAvailablePluginConfigurationException {

        String expected = "string1";

        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(PluginWithBoolean.FIELD_NAME_STRING,
                                                                           "string").dynamic(expected, "string2"));

        PluginConfiguration conf = new PluginConfiguration("",
                                                           parameters,
                                                           FreePluginWithString.class.getAnnotation(Plugin.class).id());

        PluginUtils.setup(this.getClass().getPackage().getName());
        IFreePlugin plugin = PluginUtils.getPlugin(conf,
                                                   FreePluginWithString.class.getCanonicalName(),
                                                   null,
                                                   IPluginParam.build(PluginWithBoolean.FIELD_NAME_STRING, expected));
        Assert.assertNotNull(plugin);
        Assert.assertEquals(expected, plugin.doIt());
    }
}
