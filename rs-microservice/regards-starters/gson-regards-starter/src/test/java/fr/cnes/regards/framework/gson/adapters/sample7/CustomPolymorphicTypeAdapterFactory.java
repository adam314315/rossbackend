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
package fr.cnes.regards.framework.gson.adapters.sample7;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marc Sordi
 */
@GsonTypeAdapterFactoryBean
@SuppressWarnings("rawtypes")
public class CustomPolymorphicTypeAdapterFactory extends PolymorphicTypeAdapterFactory<AbstractProperty> {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomPolymorphicTypeAdapterFactory.class);

    protected CustomPolymorphicTypeAdapterFactory() {
        super(AbstractProperty.class, "name", false);
        registerSubtype(DateProperty.class, "date");
        registerSubtype(StringProperty.class, "string");
        registerSubtype(StringProperty.class, "CRS");
        registerSubtype(ObjectProperty.class, "GEO");
    }
}
