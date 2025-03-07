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
package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.modules.model.client.IAttributeModelClient;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.model.client.IModelClient;
import fr.cnes.regards.modules.model.service.xml.IComputationPluginService;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Marc SORDI
 */
@Configuration
@PropertySource({ "classpath:metrics.properties" })
public class FeatureTestConfiguration {

    @Bean
    public IModelAttrAssocClient modelAttrAssocClient() {
        return Mockito.mock(IModelAttrAssocClient.class);
    }

    @Bean // Used in model service
    public IComputationPluginService computationPluginService() {
        return Mockito.mock(IComputationPluginService.class);
    }

    @Bean
    public IAttributeModelClient attributeModelClient() {
        return Mockito.mock(IAttributeModelClient.class);
    }

    @Bean
    public IModelClient modelClient() {
        return Mockito.mock(IModelClient.class);
    }

    @Bean
    public IStorageClient storageClient() {
        StorageClientMock mock = new StorageClientMock();
        mock.setBehavior(true, true);
        return mock;
    }
}
