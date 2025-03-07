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
package fr.cnes.regards.modules.access.services.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for calling ServicesAggregatorController methods
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
@RestClient(name = "rs-access-project", contextId = "rs-access-project.service-agg-client")
public interface IServiceAggregatorClient {

    /**
     * Returns all services applied to all datasets plus those of the given dataset
     *
     * @param datasetIpId      the id of the Dataset
     * @param applicationModes the set of {@link ServiceScope}
     * @return the list of services configured for the given dataset and the given scope
     */
    @GetMapping(value = "/services/aggregated",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EntityModel<PluginServiceDto>>> retrieveServices(
        @RequestParam(value = "datasetIpIds", required = false) final List<String> datasetIpId,
        @RequestParam(value = "applicationModes", required = false) final List<ServiceScope> applicationModes);
}
