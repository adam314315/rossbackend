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
package fr.cnes.regards.framework.utils.model;

import fr.cnes.regards.framework.utils.metamodel.MetaAttribute;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Cette classe represente une date dont la precision est a la journee
 *
 * @author Christophe Mertz
 */
public class DateAttribute extends Attribute {

    /**
     * Constructor
     */
    public DateAttribute() {
        super(new MetaAttribute(AttributeTypeEnum.TYPE_DATE));
    }

    /**
     * Ajoute une valeur a l'attribut La classe de l'objet en entree doit correspondre avec la classe de l'attribut
     *
     * @param value La nouvelle valeur de l'attribut
     */
    public void addValue(Long value) {
        Date date = new Date(value);
        super.addValue(OffsetDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")));
    }
}
