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

package fr.cnes.regards.framework.modules.plugins.dto;

import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.PluginParamType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Plugin parameter type
 *
 * @author Christophe Mertz
 */
public class PluginParamDescriptor {

    /**
     * The parameter's name used as a key for database registration
     */
    private String name;

    /**
     * A human readable label for map key. This value is only required and useful for {@link Map} type
     * parameters.
     */
    private String keyLabel;

    /**
     * A required human readable label. For {@link Map} type parameters, this label is used for map value.
     */
    private String label;

    /**
     * The parameter description, an optional further human readable information if the label is not explicit enough!
     */
    private String description;

    /**
     * The parameter markdown description for REGARDS administrators, an optional detailed human readable description.
     */
    private String markdown;

    /**
     * The parameter markdown description for REGARDS users, an optional detailed human readable description.
     */
    private String userMarkdown;

    /**
     * Argument parameter types for parameterized types
     */
    private PluginParamType[] parameterizedSubTypes;

    /**
     * The parameters's type {@link PluginParamType}.
     */
    private PluginParamType type;

    /**
     * If type is {@link PluginParamType#PLUGIN} this paramreter reprensets the type of plugin
     */
    private String pluginType;

    /**
     * A default value for the paramater
     */
    private String defaultValue;

    /**
     * Define if the parameter is optional or mandatory
     */
    private Boolean optional;

    /**
     * Define if the parameter is sensitive
     */
    private Boolean sensitive;

    /**
     * The parameters of the plugin
     */
    private List<PluginParamDescriptor> parameters;

    private Boolean unconfigurable;

    /**
     * {@link PluginParamDescriptor} builder.<br/>
     * Additional setter can be used :
     * <ul>
     * <li>{@link #setDefaultValue(String)}</li>
     * <li>{@link #addAllParameters(List)}</li>
     * <li>{@link #setParameterizedSubTypes(PluginParamType...)}</li>
     * <li>{@link #setKeyLabel(String)}</li>
     * </ul>
     *
     * @param name        parameter's name used as a key for database registration
     * @param label       a required human readable information
     * @param description an optional further human readable information if the label is not explicit enough!
     * @param paramType   {@link PluginParamType}
     * @param optional    true if parameter is optional
     * @return {@link PluginParamDescriptor}
     */
    public static PluginParamDescriptor create(String name,
                                               String label,
                                               String description,
                                               PluginParamType paramType,
                                               Boolean optional,
                                               Boolean onlyDynamic,
                                               Boolean sensitive,
                                               String pluginType) {
        PluginParamDescriptor ppt = new PluginParamDescriptor();

        // Validate and set
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException(
                "One of the plugin parameter does not have a valid name attribute within its annotation");
        }
        ppt.setName(name);

        String errorMsg = "The plugin parameter with name \"%s\" does not have a valid attribute \"%s\" within its "
                          + "annotation";
        if (label == null || label.isEmpty()) {
            throw new IllegalArgumentException(String.format(errorMsg, name, "label"));
        }
        ppt.setLabel(label);
        ppt.setDescription(description);

        if (paramType == null) {
            throw new IllegalArgumentException(String.format(errorMsg, name, "type"));
        }
        ppt.setType(paramType);

        if (optional == null) {
            throw new IllegalArgumentException(String.format(errorMsg, name, "optional"));
        }
        ppt.setOptional(optional);

        ppt.setUnconfigurable(onlyDynamic);

        ppt.setSensitive(sensitive);

        if (paramType == PluginParamType.PLUGIN) {
            ppt.setPluginType(pluginType);
        }

        return ppt;
    }

    public String getName() {
        return name;
    }

    private void setName(String pName) {
        this.name = pName;
    }

    public PluginParamType getType() {
        return type;
    }

    private void setType(PluginParamType type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        if (defaultValue == null || defaultValue.isEmpty()) {
            throw new IllegalArgumentException("Default Value is required");
        }
        this.defaultValue = defaultValue;
    }

    public Boolean isOptional() {
        return optional;
    }

    private void setOptional(Boolean optional) {
        this.optional = optional;
    }

    public List<PluginParamDescriptor> getParameters() {
        return parameters;
    }

    public void addAllParameters(List<PluginParamDescriptor> parameterTypes) {
        if (parameterTypes != null) {
            if (parameters == null) {
                parameters = new ArrayList<>();
            }
            parameters.addAll(parameterTypes);
        }
    }

    public String getLabel() {
        return label;
    }

    private void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    private void setDescription(String description) {
        this.description = description;
    }

    public PluginParamType[] getParameterizedSubTypes() {
        return parameterizedSubTypes;
    }

    public void setParameterizedSubTypes(PluginParamType... parameterizedSubTypes) {
        this.parameterizedSubTypes = parameterizedSubTypes;
    }

    public String getKeyLabel() {
        return keyLabel;
    }

    public void setKeyLabel(String keyLabel) {
        if (keyLabel == null || keyLabel.isEmpty()) {
            throw new IllegalArgumentException("Key Label is required");
        }
        this.keyLabel = keyLabel;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    public String getUserMarkdown() {
        return userMarkdown;
    }

    public void setUserMarkdown(String userMarkdown) {
        this.userMarkdown = userMarkdown;
    }

    public void setUnconfigurable(Boolean unconfigurable) {
        this.unconfigurable = unconfigurable;
    }

    public Boolean getUnconfigurable() {
        return unconfigurable;
    }

    public Boolean isSensible() {
        return sensitive;
    }

    public void setSensitive(Boolean sensitive) {
        this.sensitive = sensitive;
    }

    public String getPluginType() {
        return pluginType;
    }

    public void setPluginType(String pluginType) {
        this.pluginType = pluginType;
    }

    //    /**
    //     * An enumeration with PRIMITIVE and PLUGIN defaultValue
    //     * @author Christophe Mertz
    //     */
    //    public enum ParamType {
    //
    //        /**
    //         * Parameter type {@link Map}
    //         */
    //        MAP,
    //        /**
    //         * Parameter type {@link java.util.Collection}
    //         */
    //        COLLECTION,
    //        /**
    //         * Object type (not parameterized)
    //         */
    //        OBJECT,
    //        /**
    //         * Parameter type primitif
    //         */
    //        PRIMITIVE,
    //        /**
    //         * Parameter type plugin
    //         */
    //        PLUGIN
    //
    //    }
}
