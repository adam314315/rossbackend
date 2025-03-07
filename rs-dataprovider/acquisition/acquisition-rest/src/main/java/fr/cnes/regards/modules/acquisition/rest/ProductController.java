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
package fr.cnes.regards.modules.acquisition.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.ingest.dto.ISipState;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * {@link Product} REST module controller
 *
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(ProductController.TYPE_PATH)
public class ProductController implements IResourceController<Product> {

    public static final String TYPE_PATH = "/products";

    public static final String PRODUCT_PATH = "/{productId}";

    public static final String REQUEST_PARAM_STATE = "state";

    public static final String REQUEST_PARAM_SIP_STATE = "sipState";

    public static final String REQUEST_PARAM_PRODUCT_NAME = "productName";

    public static final String REQUEST_PARAM_CHAIN_ID = "chainId";

    public static final String REQUEST_PARAM_FROM = "from";

    public static final String REQUEST_PARAM_SESSION = "session";

    public static final String REQUEST_PARAM_NO_SESSION = "nosession";

    @Autowired
    private IProductService productService;

    /**
     * HATEOAS service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Search for {@link Product} entities matching parameters
     *
     * @param state             {@link ProductState}
     * @param sipState          {@link SIPState}
     * @param productName       {@link String}
     * @param session           {@link String}
     * @param processingChainId {@likn Long} id of {@link AcquisitionProcessingChain}
     * @param from              {@link OffsetDateTime}
     * @return {@link Product}s
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Search for products", role = DefaultRole.ADMIN)
    public ResponseEntity<PagedModel<EntityModel<Product>>> search(
        @RequestParam(name = REQUEST_PARAM_STATE, required = false) List<ProductState> state,
        @RequestParam(name = REQUEST_PARAM_SIP_STATE, required = false) List<ISipState> sipState,
        @RequestParam(name = REQUEST_PARAM_PRODUCT_NAME, required = false) String productName,
        @RequestParam(name = REQUEST_PARAM_SESSION, required = false) String session,
        @RequestParam(name = REQUEST_PARAM_CHAIN_ID, required = false) Long processingChainId,
        @RequestParam(name = REQUEST_PARAM_NO_SESSION, required = false) Boolean noSession,
        @RequestParam(name = REQUEST_PARAM_FROM, required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime from,
        @PageableDefault(sort = "lastUpdate", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<Product> assembler) {
        Page<Product> products = productService.search(state,
                                                       sipState,
                                                       productName,
                                                       session,
                                                       processingChainId,
                                                       from,
                                                       noSession,
                                                       pageable);
        return new ResponseEntity<>(toPagedResources(products, assembler), HttpStatus.OK);
    }

    /**
     * Retreive a {@link Product} by id
     */
    @RequestMapping(method = RequestMethod.GET, value = PRODUCT_PATH)
    @ResourceAccess(description = "Get a product", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<Product>> get(@PathVariable Long productId) throws ModuleException {
        return ResponseEntity.ok(toResource(productService.loadProduct(productId)));
    }

    @Override
    public EntityModel<Product> toResource(Product element, Object... extras) {
        return resourceService.toResource(element);
    }

}
