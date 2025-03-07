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
package fr.cnes.regards.modules.dam.service.entities;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.dam.dao.entities.*;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import fr.cnes.regards.modules.dam.domain.entities.feature.CollectionFeature;
import fr.cnes.regards.modules.dam.service.settings.IDamSettingsService;
import fr.cnes.regards.modules.model.service.IModelService;
import fr.cnes.regards.modules.model.service.validation.IModelFinder;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

/**
 * Specific EntityService for collections
 *
 * @author Sylvain Vissiere-Guerinet
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class CollectionService extends AbstractEntityService<CollectionFeature, Collection>
    implements ICollectionService {

    public CollectionService(IModelFinder modelFinder,
                             IAbstractEntityRepository<AbstractEntity<?>> pEntityRepository,
                             IModelService pModelService,
                             IPluginService pluginService,
                             IDamSettingsService damSettingsService,
                             IDeletedEntityRepository pDeletedEntityRepository,
                             ICollectionRepository pCollectionRepository,
                             IDatasetRepository pDatasetRepository,
                             EntityManager pEm,
                             IPublisher pPublisher,
                             IRuntimeTenantResolver runtimeTenantResolver,
                             IAbstractEntityRequestRepository abstractEntityRequestRepo) {
        super(modelFinder,
              pEntityRepository,
              pModelService,
              pluginService,
              damSettingsService,
              pDeletedEntityRepository,
              pCollectionRepository,
              pDatasetRepository,
              pCollectionRepository,
              pEm,
              pPublisher,
              runtimeTenantResolver,
              abstractEntityRequestRepo);
    }

}
