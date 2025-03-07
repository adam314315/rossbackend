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
package fr.cnes.regards.modules.crawler.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.service.ICrawlerAndIngesterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Crawler rest controller
 *
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(CrawlerController.TYPE_MAPPING)
public class CrawlerController implements IResourceController<DatasourceIngestion> {

    public static final String TYPE_MAPPING = "/crawler/datasourceIngestions";

    public static final String INGESTION_ID = "/{ingestion_id}";

    /**
     * Crawler service
     */
    @Autowired
    private ICrawlerAndIngesterService crawlerService;

    /**
     * HATEOAS service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Retrieve all DatasourceIngestion.
     *
     * @return a list of DatasourceIngestion
     */
    @GetMapping
    @Operation(summary = "Get crawler datasources", description = "Return a list of crawler datasources")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "All crawler datasources were retrieved.") })
    @ResourceAccess(description = "List all crawler datasources.")
    public ResponseEntity<List<EntityModel<DatasourceIngestion>>> getAllDatasourceIngestion() {
        return ResponseEntity.ok(toResources(crawlerService.getDatasourceIngestions()));
    }

    /**
     * Delete a DatasourceIngestion.
     *
     * @param ingestionId {@link DatasourceIngestion} id
     * @return {@link Void}
     */
    @ResourceAccess(description = "Delete selected datasource.")
    @RequestMapping(method = RequestMethod.DELETE, value = INGESTION_ID)
    public ResponseEntity<Void> deleteDatasourceIngestion(@PathVariable("ingestion_id") String ingestionId) {
        crawlerService.deleteDatasourceIngestion(ingestionId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Schedule datasource ingestion to be executed as soon as possible
     *
     * @param ingestionId {@link DatasourceIngestion} id
     * @return {@link Void}
     */
    @ResourceAccess(description = "Schedule datasource to be ingested as soon as possible.")
    @RequestMapping(method = RequestMethod.PUT, value = INGESTION_ID)
    public ResponseEntity<Void> scheduleNowDatasourceIngestion(@PathVariable("ingestion_id") String ingestionId) {
        crawlerService.scheduleNowDatasourceIngestion(ingestionId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public EntityModel<DatasourceIngestion> toResource(DatasourceIngestion element, Object... extras) {
        EntityModel<DatasourceIngestion> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                this.getClass(),
                                "deleteDatasourceIngestion",
                                LinkRels.DELETE,
                                MethodParamFactory.build(String.class, element.getId()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "scheduleNowDatasourceIngestion",
                                LinkRelation.of("SCHEDULE"),
                                MethodParamFactory.build(String.class, element.getId()));
        return resource;
    }

}
