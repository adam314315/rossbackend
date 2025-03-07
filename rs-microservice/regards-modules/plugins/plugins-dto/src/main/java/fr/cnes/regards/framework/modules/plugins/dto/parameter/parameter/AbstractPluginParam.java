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

package fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Parameter associated to a plugin configuration
 *
 * @param <T> parameter type
 * @author Christophe Mertz
 * @author Marc SORDI
 */
public abstract class AbstractPluginParam<T> implements IPluginParam {

    protected String name;

    protected PluginParamType type;

    protected T value;

    protected boolean dynamic = false;

    protected Set<T> dynamicsValues;

    public AbstractPluginParam(PluginParamType type) {
        this.type = type;
    }

    @Override
    public boolean hasValue() {
        return value != null;
    }

    @Override
    public PluginParamType getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isValid(IPluginParam dynamicParam) {
        if (this.getClass().isInstance(dynamicParam)) {
            return isValidDynamicValue((T) dynamicParam.getValue());
        }
        return false;
    }

    public boolean isValidDynamicValue(T value) {
        if ((dynamicsValues == null) || dynamicsValues.isEmpty()) {
            // No restriction
            return true;
        } else {
            for (T dyn : dynamicsValues) {
                if (dyn.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasDynamicValues() {
        return (dynamicsValues != null) && !dynamicsValues.isEmpty();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (name == null ? 0 : name.hashCode());
        return result;
    }

    /**
     * The name of the parameter is the natural id. Two plugin parameters can have the same name but not within same
     * plugin configuration
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("rawtypes") AbstractPluginParam other = (AbstractPluginParam) obj;

        if (name == null) {
            return other.name == null;
        } else {
            return name.equals(other.name);
        }
    }

    @Override
    public String toString() {
        return name + " - " + value + " - " + dynamic;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public Set<T> getDynamicsValues() {
        return dynamicsValues;
    }

    public void setDynamicsValues(Set<T> dynamicsValues) {
        this.dynamicsValues = dynamicsValues;
    }

    // Fluent API

    @SuppressWarnings("unchecked")
    public <P extends AbstractPluginParam<T>> P with(String name, T value) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Plugin parameter name is required");
        }
        if (value == null) {
            throw new IllegalArgumentException("Plugin parameter value is required");
        }
        this.name = name;
        this.value = value;
        return (P) this;
    }

    @SuppressWarnings("unchecked")
    public <P extends AbstractPluginParam<T>> P with(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Plugin parameter name is required");
        }
        this.name = name;
        return (P) this;
    }

    public AbstractPluginParam<T> dynamic() {
        this.setDynamic(Boolean.TRUE);
        return this;
    }

    public AbstractPluginParam<T> dynamic(Set<T> dynamicsValues) {
        this.setDynamic(Boolean.TRUE);
        this.setDynamicsValues(dynamicsValues);
        return this;
    }

    public AbstractPluginParam<T> dynamic(T... dyns) {
        this.setDynamic(Boolean.TRUE);
        this.setDynamicsValues(new HashSet<>(Arrays.asList(dyns)));
        return this;
    }

    @Override
    public void toStatic() {
        this.setDynamic(Boolean.FALSE);
        this.setDynamicsValues(null);
    }
}
