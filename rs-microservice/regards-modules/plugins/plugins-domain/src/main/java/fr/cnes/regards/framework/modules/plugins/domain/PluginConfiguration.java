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

package fr.cnes.regards.framework.modules.plugins.domain;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.dto.PluginConfigurationDto;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.AbstractPluginParam;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import io.vavr.control.Option;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.net.URL;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Plugin configuration contains a unique Id, plugin meta-data and parameters.
 *
 * @author cmertz
 * @author oroussel
 */

@Entity
@Table(name = "t_plugin_configuration",
       indexes = { @Index(name = "idx_plugin_configuration", columnList = "pluginId"),
                   @Index(name = "idx_plugin_configuration_label", columnList = "label"),
                   @Index(name = "idx_plugin_configuration_bid", columnList = "bid") },
       uniqueConstraints = @UniqueConstraint(name = "uk_plugin_bid", columnNames = { "bid" }))
@SequenceGenerator(name = "pluginConfSequence", initialValue = 1, sequenceName = "seq_plugin_conf")
public class PluginConfiguration implements IIdentifiable<Long> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginConfiguration.class);

    /**
     * A constant used to define a {@link String} constraint with length 255
     */
    private static final int MAX_STRING_LENGTH = 255;

    public static final String BID_REGEXP = "[0-9a-zA-Z_-]*";

    /**
     * Unique id
     */
    @ConfigIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pluginConfSequence")
    private Long id;

    @GsonIgnore
    @Transient
    private PluginMetaData metaData;

    /**
     * Unique identifier of the plugin. This id is the id defined in the "@Plugin" annotation of the plugin
     * implementation class.
     * <b>DO NOT SET @NotNull even if pluginId must not be null. Validation is done between front and back but at
     * creation, pluginId is retrieved from plugin metadata and thus set by back</b>
     */
    @Column(nullable = false)
    private String pluginId;

    /**
     * Label to identify the configuration.
     */
    @NotBlank(message = "the label cannot be blank")
    @Column(name = "label", length = MAX_STRING_LENGTH)
    private String label;

    /**
     * A serialized {@link UUID} for business identifier
     */
    @Pattern(regexp = BID_REGEXP,
             message = "Business identifier must conform to regular expression \"" + BID_REGEXP + "\".")
    @Column(name = "bid", length = 36, nullable = false, updatable = false)
    private String businessId;

    /**
     * Version of the plugin configuration. Is set with the plugin version. This attribute is used to check if the saved
     * configuration plugin version differs from the loaded plugin.
     * <b>DO NOT SET @NotNull even if version must not be null. Validation is done between front and back but at
     * creation, version is retrieved from plugin metadata and thus set by back</b>
     */
    @Column(nullable = false, updatable = true)
    private String version;

    /**
     * Priority order of the plugin.
     */
    @NotNull(message = "the priority order cannot be null")
    @Column(nullable = false, updatable = true)
    private Integer priorityOrder = 0;

    /**
     * The plugin configuration is active.
     */
    private Boolean active = true;

    /**
     * Configuration parameters of the plugin
     */
    @Valid
    @Column(columnDefinition = "jsonb")
    @Type(value = JsonBinaryType.class,
          parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE,
                                    value = "fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam") })
    private Set<IPluginParam> parameters = Sets.newHashSet();

    /**
     * Icon of the plugin. It must be an URL to a svg file.
     */
    @Column(name = "icon_url")
    private URL iconUrl;

    /**
     * Default constructor
     */
    public PluginConfiguration() {
        super();
    }

    /**
     * A constructor with {@link PluginMetaData}.
     *
     * @param label the label
     */
    public PluginConfiguration(String label, String pluginId) {
        this(label, Lists.newArrayList(), 0, pluginId);
    }

    /**
     * A constructor with {@link PluginMetaData} and list of {@link AbstractPluginParam}.
     *
     * @param label      the label
     * @param parameters the list of parameters
     */
    public PluginConfiguration(String label, Set<IPluginParam> parameters, String pluginId) {
        this(label, parameters, 0, pluginId);
    }

    /**
     * A constructor with {@link PluginMetaData}.
     *
     * @param label the label
     * @param order the order
     */
    public PluginConfiguration(String label, int order, String pluginId) {
        this(label, Lists.newArrayList(), order, pluginId);
    }

    /**
     * Main constructor
     *
     * @param label      the label
     * @param parameters the list of parameters
     * @param order      the order
     * @param pluginId   plugin identifier
     */
    public PluginConfiguration(String label, Collection<IPluginParam> parameters, int order, String pluginId) {
        super();
        this.businessId = UUID.randomUUID().toString();
        if (parameters != null) {
            this.parameters.addAll(parameters);
        }
        this.pluginId = pluginId;
        priorityOrder = order;
        this.label = label;
        active = Boolean.TRUE;
    }

    public PluginConfiguration(String pluginId,
                               String label,
                               String businessId,
                               String version,
                               Integer priorityOrder,
                               Boolean active,
                               URL iconUrl,
                               Set<IPluginParam> parameters,
                               PluginMetaData metaData) {
        this.pluginId = pluginId;
        this.label = label;
        this.businessId = businessId;
        this.version = version;
        this.priorityOrder = priorityOrder;
        this.active = active;
        this.iconUrl = iconUrl;
        this.parameters = parameters;
        this.metaData = metaData;
    }

    /**
     * Build a new plugin configuration
     *
     * @param pluginId   the {@link Plugin#id()}.
     * @param label      the configuration label (if <code>null</code>, a random label is generated)
     * @param parameters the list of parameters
     * @param order      the order
     */
    public static PluginConfiguration build(String pluginId,
                                            @Nullable String label,
                                            @Nullable Collection<IPluginParam> parameters,
                                            int order) {
        if (label == null) {
            label = UUID.randomUUID().toString();
        }
        return new PluginConfiguration(label, parameters, 0, pluginId);
    }

    /**
     * Build a new plugin configuration
     *
     * @param pluginId   the {@link Plugin#id()}.
     * @param label      the configuration label (if <code>null</code>, a random label is generated)
     * @param parameters the list of parameters
     */
    public static PluginConfiguration build(String pluginId,
                                            @Nullable String label,
                                            @Nullable Collection<IPluginParam> parameters) {
        return build(pluginId, label, parameters, 0);
    }

    /**
     * Build a new plugin configuration
     *
     * @param pluginType the {@link Class} annotated with {@link Plugin}.
     * @param label      the configuration label (if <code>null</code>, a random label is generated)
     * @param parameters the list of parameters
     */
    public static PluginConfiguration build(Class<?> pluginType,
                                            @Nullable String label,
                                            @Nullable Collection<IPluginParam> parameters) {
        Plugin plugin = pluginType.getAnnotation(Plugin.class);
        if (plugin == null) {
            throw new IllegalArgumentException(String.format(
                "Plugin type \"%s\" must be annotated with plugin annotation",
                pluginType.getName()));
        }
        return build(plugin.id(), label, parameters, 0);
    }

    public PluginMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(PluginMetaData metaData) {
        this.metaData = metaData;
    }

    public void setMetaDataAndPluginId(PluginMetaData metaData) {
        // For serialization
        Option.of(metaData).peek(m -> pluginId = m.getPluginId());
        // Transient information only useful at runtime
        this.metaData = metaData;
    }

    /**
     * Return the {@link IPluginParam} of a specific parameter
     *
     * @param name the parameter to get the value
     * @return {@link IPluginParam} or null
     */
    public IPluginParam getParameter(String name) {
        for (IPluginParam p : parameters) {
            if ((p != null) && p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * @param name parameter name
     * @return its value or null
     */
    public Object getParameterValue(String name) {
        for (IPluginParam p : parameters) {
            if ((p != null) && p.getName().equals(name)) {
                return p.getValue();
            }
        }
        return null;
    }

    /**
     * Log the {@link AbstractPluginParam} of the {@link PluginConfiguration}.
     */
    public void logParams() {
        LOGGER.info("===> parameters <===");
        LOGGER.info("  ---> number of dynamic parameters : " + getParameters().stream()
                                                                              .filter(IPluginParam::isDynamic)
                                                                              .count());

        getParameters().stream()
                       .filter(IPluginParam::isDynamic)
                       .forEach(p -> LOGGER.info("  ---> dynamic parameter : {}-def val: {}",
                                                 p.getName(),
                                                 p.toString()));

        LOGGER.info("  ---> number of no dynamic parameters : " + getParameters().stream()
                                                                                 .filter(p -> !p.isDynamic())
                                                                                 .count());
        getParameters().stream()
                       .filter(p -> !p.isDynamic())
                       .forEach(p -> LOGGER.info("  ---> parameter : {}-def val: {}", p.getName(), p.toString()));
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String pLabel) {
        label = pLabel;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public Integer getPriorityOrder() {
        return priorityOrder;
    }

    public void setPriorityOrder(Integer order) {
        priorityOrder = order;
    }

    public Set<IPluginParam> getParameters() {
        return parameters;
    }

    public void setParameters(Set<IPluginParam> parameters) {
        this.parameters.clear();
        if ((parameters != null) && !parameters.isEmpty()) {
            this.parameters.addAll(parameters);
        }
    }

    public Boolean isActive() {
        return active;
    }

    public void setIsActive(Boolean pIsActive) {
        active = pIsActive;
    }

    public String getPluginClassName() {
        return metaData.getPluginClassName();
    }

    public Set<String> getInterfaceNames() {
        return metaData.getInterfaceNames();
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    /**
     * @return the iconUrl
     */
    public URL getIconUrl() {
        return iconUrl;
    }

    /**
     * @param pIconUrl the iconUrl to set
     */
    public void setIconUrl(URL pIconUrl) {
        iconUrl = pIconUrl;
    }

    public void generateBusinessIdIfNotSet() {
        if (this.businessId == null) {
            this.businessId = UUID.randomUUID().toString();
        }
    }

    public void resetBusinessId() {
        this.businessId = null;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    @Override
    public String toString() {
        return "PluginConfiguration [id="
               + id
               + ", business id="
               + businessId
               + ", pluginId="
               + pluginId
               + ", label="
               + label
               + ", version="
               + version
               + ", priorityOrder="
               + priorityOrder
               + ", active="
               + active
               + "]";
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = (prime * result) + (businessId == null ? 0 : businessId.hashCode());
        result = (prime * result) + (pluginId == null ? 0 : pluginId.hashCode());
        return result;
    }

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
        PluginConfiguration other = (PluginConfiguration) obj;
        if (businessId == null) {
            if (other.businessId != null) {
                return false;
            }
        } else if (!businessId.equals(other.businessId)) {
            return false;
        }
        if (pluginId == null) {
            if (other.pluginId != null) {
                return false;
            }
        } else if (!pluginId.equals(other.pluginId)) {
            return false;
        }
        return true;
    }

    public PluginConfigurationDto toDto() {
        return new PluginConfigurationDto(pluginId,
                                          label,
                                          businessId,
                                          version,
                                          priorityOrder,
                                          active,
                                          iconUrl,
                                          parameters,
                                          metaData);
    }

    public static PluginConfiguration fromDto(PluginConfigurationDto dto) {
        return new PluginConfiguration(dto.getPluginId(),
                                       dto.getLabel(),
                                       dto.getBusinessId(),
                                       dto.getVersion(),
                                       dto.getPriorityOrder(),
                                       dto.isActive(),
                                       dto.getIconUrl(),
                                       dto.getParameters(),
                                       dto.getMetaData());
    }

}
