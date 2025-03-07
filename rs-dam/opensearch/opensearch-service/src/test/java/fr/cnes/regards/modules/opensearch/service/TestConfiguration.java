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

import fr.cnes.regards.modules.model.client.IAttributeModelClient;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.model.gson.IAttributeHelper;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.toponyms.client.IToponymsClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author sbinda
 */
@Configuration
public class TestConfiguration {

    @Bean
    @Primary
    IAttributeFinder finder() {
        return Mockito.mock(IAttributeFinder.class);
    }

    @Bean
    @Primary
    IAttributeModelClient attMClient() {
        return Mockito.mock(IAttributeModelClient.class);
    }

    @Bean
    @Primary
    IModelAttrAssocClient modelAttMClient() {
        return Mockito.mock(IModelAttrAssocClient.class);
    }

    @Bean
    @Primary
    IAttributeHelper helper() {
        return Mockito.mock(IAttributeHelper.class);
    }

    @Bean
    @Primary
    IToponymsClient toponymClient() {
        return Mockito.mock(IToponymsClient.class);
    }

}
