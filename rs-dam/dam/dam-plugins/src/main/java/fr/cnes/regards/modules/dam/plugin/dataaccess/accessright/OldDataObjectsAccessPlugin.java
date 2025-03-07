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
package fr.cnes.regards.modules.dam.plugin.dataaccess.accessright;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessright.plugins.IDataObjectAccessFilterPlugin;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;

import java.time.OffsetDateTime;

/**
 * Plugin to allow access to old dataobjects.
 *
 * @author Sébastien Binda
 */
@Plugin(id = "OldDataObjectsAccess",
        version = "4.0.0-SNAPSHOT",
        description = "Allow access only to old data objects. Old data objects are thoses created at least X days ago. X is a parameter to configure.",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class OldDataObjectsAccessPlugin implements IDataObjectAccessFilterPlugin {

    public static final String NB_DAYS_PARAM = "numberOfDays";

    public static final String DATE_ATTR_PARAM = "dateAttribute";

    @PluginParameter(name = NB_DAYS_PARAM, label = "Number of days", optional = false)
    private long numberOfDays;

    @PluginParameter(name = DATE_ATTR_PARAM,
                     label = "Date attribute",
                     description = "It's the model's attribute's name with the fragment if there is. The model contains the date. In order to use an attribute from the meta of your datas, use the prefix \"feature.properties\" like \"feature.properties.DataStartDate\". Without this prefix you can access regards internal meta attributes like default selected date attribute \"creationDate\"",
                     defaultValue = "creationDate",
                     optional = true)
    private String dateAttribute;

    @Override
    public ICriterion getSearchFilter() {
        OffsetDateTime time = OffsetDateTime.now().minusDays(numberOfDays);
        return ICriterion.or(ICriterion.lt(dateAttribute, time), ICriterion.eq(dateAttribute, time));
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

}
