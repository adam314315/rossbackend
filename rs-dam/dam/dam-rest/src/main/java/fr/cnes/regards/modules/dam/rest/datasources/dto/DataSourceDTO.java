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
package fr.cnes.regards.modules.dam.rest.datasources.dto;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

/**
 * DTO to add number of associated datasets to a data source {@link PluginConfiguration}
 *
 * @author sbinda
 */
public class DataSourceDTO extends PluginConfiguration {

    private Long associatedDatasets = 0L;

    public DataSourceDTO(Long associatedDatasets, PluginConfiguration dataSourceConf) {
        super();
        this.associatedDatasets = associatedDatasets;
        super.setId(dataSourceConf.getId());
        super.setBusinessId(dataSourceConf.getBusinessId());
        super.setPluginId(dataSourceConf.getPluginId());
        super.setLabel(dataSourceConf.getLabel());
        super.setVersion(dataSourceConf.getVersion());
        super.setPriorityOrder(dataSourceConf.getPriorityOrder());
        super.setIsActive(dataSourceConf.isActive());
        super.setMetaDataAndPluginId(dataSourceConf.getMetaData());
        super.setParameters(dataSourceConf.getParameters());
        super.setIconUrl(dataSourceConf.getIconUrl());
    }

    /**
     * @return the associatedDatasets
     */
    public Long getAssociatedDatasets() {
        return associatedDatasets;
    }

    /**
     * @param associatedDatasets the associatedDatasets to set
     */
    public void setAssociatedDatasets(Long associatedDatasets) {
        this.associatedDatasets = associatedDatasets;
    }

}
