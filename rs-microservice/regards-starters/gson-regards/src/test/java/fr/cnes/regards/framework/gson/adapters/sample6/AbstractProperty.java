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
package fr.cnes.regards.framework.gson.adapters.sample6;

import fr.cnes.regards.framework.gson.annotation.Gsonable;

/**
 * @param <T> property type
 * @author Marc Sordi
 */
@Gsonable
public abstract class AbstractProperty<T> implements IProperty<T> {

    /**
     * sample field
     */
    protected String name;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }
}
