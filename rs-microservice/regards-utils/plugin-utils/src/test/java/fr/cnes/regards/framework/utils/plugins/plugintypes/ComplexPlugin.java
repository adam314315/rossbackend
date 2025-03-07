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
package fr.cnes.regards.framework.utils.plugins.plugintypes;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.basic.ISamplePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ISamplePlugin
 *
 * @author Christophe Mertz
 */
@Plugin(description = "Complex plugin test",
        id = "aComplexPlugin",
        version = "0.0.1",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class ComplexPlugin implements ISamplePlugin {

    // Field names
    public static final String FIELD_NAME_PLUGIN = "complexInterfacePlugin";

    public static final String FIELD_NAME_COEF = "coef";

    public static final String FIELD_NAME_ACTIVE = "isActive";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexPlugin.class);

    /**
     * A plugin with an annotation {@link PluginParameter}
     */
    @PluginParameter(description = "Plugin interface", label = "Embedded plugin")
    private IComplexInterfacePlugin complexInterfacePlugin;

    /**
     * A {@link Integer} parameter
     */
    @PluginParameter(description = "int parameter", label = "Coeff")
    private final Integer coef = 0;

    /**
     * A {@link Boolean} parameter
     */
    @PluginParameter(description = "boolean parameter", label = "Enabled")
    private final Boolean isActive = Boolean.FALSE;

    @Override
    public String echo(final String pMessage) {
        final StringBuilder str = new StringBuilder();
        if (isActive) {
            str.append(this.getClass().getName()).append("-").append(pMessage);
        } else {

            str.append(this.getClass().getName()).append(":is not active");
        }
        return str.toString();
    }

    @Override
    public int add(final int pFirst, final int pSecond) {
        final float f = complexInterfacePlugin.mult(4, 8);
        LOGGER.info("float=" + f);
        final int res = coef * (pFirst + pSecond);
        LOGGER.info("add result : " + res);
        return res;
    }

    public String echoPluginParameter() {
        return complexInterfacePlugin.toString();
    }

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + this.getClass().getName() + "|active:" + isActive + "|coeff:" + coef);
        // + "|plg_conf:" + this.pluginConfiguration.getId()+ "|plg_int:" + this.complexInterfacePlugin.toString()
    }
}
