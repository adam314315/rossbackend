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
package fr.cnes.regards.modules.crawler.test;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.autoconfigure.MethodAuthorizationServiceAutoConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.MethodSecurityAutoConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.SecurityVoterAutoConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.WebSecurityAutoConfiguration;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dam.service.dataaccess.IAccessRightService;
import fr.cnes.regards.modules.indexer.dao.spatial.ProjectGeoSettings;
import fr.cnes.regards.modules.model.client.IAttributeModelClient;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;
import fr.cnes.regards.modules.toponyms.client.IToponymsClient;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;

@Profile("!indexer-service")
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.modules.crawler.service",
                                "fr.cnes.regards.modules.indexer",
                                "fr.cnes.regards.modules.dam",
                                "fr.cnes.regards.modules.model",
                                "fr.cnes.regards.modules.search",
                                "fr.cnes.regards.framework.modules.plugins.service",
                                "fr.cnes.regards.framework.utils.spring" })
@EnableAutoConfiguration(exclude = { MethodAuthorizationServiceAutoConfiguration.class,
                                     MethodSecurityAutoConfiguration.class,
                                     SecurityVoterAutoConfiguration.class,
                                     WebSecurityAutoConfiguration.class })
public class CrawlerConfiguration {

    @Bean
    public IAttributeModelClient attributeModelClient() {
        return Mockito.mock(IAttributeModelClient.class);
    }

    @Bean
    @Primary
    public IOpenSearchService openSearchService() {
        return Mockito.mock(IOpenSearchService.class);
    }

    @Bean
    public IAccessRightService getAccessRightsService() {
        return Mockito.mock(IAccessRightService.class);
    }

    @Bean
    public IProjectsClient projectsClient() {
        return Mockito.mock(IProjectsClient.class);
    }

    @Bean
    public IProjectUsersClient projectUsersClient() {
        return Mockito.mock(IProjectUsersClient.class);
    }

    @Bean
    public IModelAttrAssocClient modelAttrAssocClient() {
        return Mockito.mock(IModelAttrAssocClient.class);
    }

    @Bean
    public IResourceService getResourceService() {
        return Mockito.mock(IResourceService.class);
    }

    @Bean
    public IAttributeFinder attributeFinder() {
        return Mockito.mock(IAttributeFinder.class);
    }

    @Bean
    public IStorageClient storageClient() {
        return Mockito.mock(IStorageClient.class);
    }

    @Bean
    public IStorageRestClient storageRestClient() {
        return Mockito.mock(IStorageRestClient.class);
    }

    @Bean
    public IStorageSettingClient storageSettingClient() {
        return Mockito.mock(IStorageSettingClient.class);
    }

    @Bean
    public IToponymsClient toponymClient() {
        return Mockito.mock(IToponymsClient.class);
    }

    @Primary
    @Bean
    public ProjectGeoSettings getProjectGeoSettings() {
        ProjectGeoSettings p = Mockito.mock(ProjectGeoSettings.class);
        return p;
    }
}
