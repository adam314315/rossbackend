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
package fr.cnes.regards.modules.opensearch.service;

import com.google.common.collect.Lists;
import feign.FeignException;
import feign.Target;
import feign.Target.HardCodedTarget;
import feign.httpclient.ApacheHttpClient;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;
import fr.cnes.regards.modules.opensearch.service.parser.*;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.UrlType;
import fr.cnes.regards.modules.search.schema.parameters.OpenSearchParameter;
import fr.cnes.regards.modules.toponyms.client.IToponymsClient;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Parses generic OpenSearch requests like
 * <code>q={searchTerms}&lat={geo:lat?}&lon={geo:lon?}&r={geo:radius?}&g=POLYGON((0.582%2040.496%2C%200.231%2040.737%2C%200.736%2042.869%2C%203.351%2042.386%2C%203.263%2041.814%2C%202.164%2041.265%2C%200.978%20%20%2040.957%2C%200.802%2040.781%2C%200.978%2040.649%2C%200.582%2040.496))</code>
 * <p>
 * It is coded so that you can add as many parsers as you want, each handling a specific part of the request.
 * You just need to implement a new {@link IParser}, and register it in the <code>aggregate</code> method.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
public class OpenSearchService implements IOpenSearchService {

    // Class logger
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchService.class);

    // Thread safe parsers holder
    private static ThreadLocal<List<IParser>> parsersHolder;

    /**
     * Default HttpClient if none is found
     */
    private final HttpClient httpClient;

    public OpenSearchService(IAttributeFinder finder, HttpClient httpClient, IToponymsClient toponymClient) {
        OpenSearchService.parsersHolder = ThreadLocal.withInitial(() -> Lists.newArrayList(new QueryParser(finder),
                                                                                           new GeometryParser(),
                                                                                           new CircleParser(),
                                                                                           new FieldExistsParser(),
                                                                                           new ImageOnlyParser(),
                                                                                           new ToponymParser(
                                                                                               toponymClient)));
        this.httpClient = httpClient;
    }

    @Override
    public ICriterion parse(MultiValueMap<String, String> queryParameters) throws OpenSearchParseException {
        List<ICriterion> criteria = new ArrayList<>();
        if (queryParameters != null) {
            for (IParser parser : parsersHolder.get()) {
                // Parse parameters ... may return null if parser required parameter(s) not set
                ICriterion crit = parser.parse(queryParameters);
                if ((crit != null) && !crit.isEmpty()) {
                    criteria.add(crit);
                }
            }
        }
        return criteria.isEmpty() ? ICriterion.all() : ICriterion.and(criteria);
    }

    @Override
    public OpenSearchDescription readDescriptor(URL url) throws ModuleException {
        Target<IOpensearchDescriptorClient> target = new HardCodedTarget<>(IOpensearchDescriptorClient.class,
                                                                           url.toString());
        try {
            ResponseEntity<OpenSearchDescription> descriptor = FeignClientBuilder.buildXml(target,
                                                                                           new ApacheHttpClient(
                                                                                               httpClient))
                                                                                 .getDescriptor();
            if (descriptor.getStatusCode() == HttpStatus.OK) {
                OpenSearchDescription openSearchDescription = descriptor.getBody();
                if (openSearchDescription != null && !openSearchDescription.getUrl().isEmpty()) {
                    return removeDuplicatedParameters(openSearchDescription);
                } else {
                    throw new ModuleException(String.format("No valid opensearch descriptor found at %s.", url));
                }
            } else {
                throw new ModuleException(String.format("Error retrieving opensearch descriptor at %s.", url));
            }
        } catch (HttpClientErrorException | HttpServerErrorException | FeignException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    private OpenSearchDescription removeDuplicatedParameters(OpenSearchDescription descriptor) {
        descriptor.getUrl().forEach(url -> {
            List<OpenSearchParameter> uniqParameters = Lists.newArrayList();
            Iterator<OpenSearchParameter> it = url.getParameter().iterator();
            while (it.hasNext()) {
                OpenSearchParameter param = it.next();
                if (uniqParameters.stream().anyMatch(p -> p.getName().equals(param.getName()))) {
                    it.remove();
                    LOGGER.warn("Removing duplicated attribute {} from opensearch descriptor", param.getName());
                } else {
                    uniqParameters.add(param);
                }
            }
        });
        return descriptor;
    }

    @Override
    public UrlType getSearchRequestURL(OpenSearchDescription descriptor, MediaType type) throws ModuleException {
        return descriptor.getUrl()
                         .stream()
                         .filter(template -> type.toString().equals(template.getType()))
                         .findFirst()
                         .orElseThrow(() -> new ModuleException("No Template url matching"));
    }

}
