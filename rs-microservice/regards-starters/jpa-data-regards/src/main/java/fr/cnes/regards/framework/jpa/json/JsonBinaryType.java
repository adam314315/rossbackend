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
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
/*
 * Copyright 2015 Vlad Mihalcea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.regards.framework.jpa.json;

import java.util.Properties;

import io.hypersistence.utils.hibernate.type.MutableDynamicParameterizedType;
import io.hypersistence.utils.hibernate.type.json.internal.JsonBinaryJdbcTypeDescriptor;

/**
 * In order to use mapped object in Jsonb using Gson instead of Jackson, it is necessary to define our specific JsonBinaryType based on Vlad Mihalcea
 * one (from io.hypersistence.hypersistence-utils-hibernate-62, 62 for Hibernate 6.2).
 * This is a UserType inheriting MutableDynamicParameterizedType so it can be used for relatively complex mapping such as parameterized set or list.
 * Hibernate mapping example :
 * <pre><code>
 *     @Column(columnDefinition = "jsonb")
 *     @Type(value = JsonBinaryType.class,
 *           parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE,
 *                                     value = "fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam") })
 *     private Set<IPluginParam> parameters = Sets.newHashSet();
 * </code></pre>
 *
 * @author Vlad MIhalcea
 */
public class JsonBinaryType
    extends MutableDynamicParameterizedType<Object, JsonBinaryJdbcTypeDescriptor, JsonTypeDescriptor> {

    public JsonBinaryType() {
        super(Object.class, JsonBinaryJdbcTypeDescriptor.INSTANCE, new JsonTypeDescriptor());

    }

    public String getName() {
        return "jsonb";
    }

    @Override
    public void setParameterValues(Properties parameters) {
        getJavaTypeDescriptor().setParameterValues(parameters);
    }

}