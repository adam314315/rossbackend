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
package fr.cnes.regards.framework.gson.adapters.sample2;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;

/**
 * @author Marc Sordi
 */
public class AnimalAdapterFactory2 extends PolymorphicTypeAdapterFactory<Animal> {

    public AnimalAdapterFactory2(boolean pInjectField) {
        super(Animal.class, "type", pInjectField);
        registerSubtype(Hawk.class, "bird");
        registerSubtype(Lion.class, "mammal");
        registerSubtype(Shark.class, "fish");
    }

}
