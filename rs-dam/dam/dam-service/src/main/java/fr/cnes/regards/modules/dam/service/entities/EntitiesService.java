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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.dam.dao.entities.IAbstractEntityRepository;
import fr.cnes.regards.modules.dam.dao.entities.ICollectionRepository;
import fr.cnes.regards.modules.dam.dao.entities.IDatasetRepository;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.model.domain.IComputedAttribute;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.service.IModelAttrAssocService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Unparameterized entity service. This service is used when the entity type is unknown (ex. CrawlerService)
 *
 * @author oroussel
 * @author Christophe Mertz
 */
@Service
@MultitenantTransactional
public class EntitiesService implements IEntitiesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntitiesService.class);

    @Autowired
    private IDatasetRepository datasetRepository;

    @Autowired
    private IAbstractEntityRepository<AbstractEntity<?>> entityRepository;

    @Autowired
    private IModelAttrAssocService modelAttributeService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ICollectionRepository collectionRepository;

    public EntitiesService() {
        super();
    }

    @Override
    public AbstractEntity<?> loadWithRelations(UniformResourceName ipId) {
        // Particular case on datasets and collections which contains more relations
        if (ipId.getEntityType() == EntityType.DATASET) {
            return datasetRepository.findByIpId(ipId);
        }
        if (ipId.getEntityType() == EntityType.COLLECTION) {
            return collectionRepository.findByIpId(ipId);
        }
        return entityRepository.findByIpId(ipId);
    }

    @Override
    public List<AbstractEntity<?>> loadAllWithRelations(UniformResourceName... ipIds) {
        List<AbstractEntity<?>> entities = new ArrayList<>(ipIds.length);
        Set<UniformResourceName> dsUrns = Arrays.stream(ipIds)
                                                .filter(ipId -> ipId.getEntityType() == EntityType.DATASET)
                                                .collect(Collectors.toSet());
        if (!dsUrns.isEmpty()) {
            entities.addAll(datasetRepository.findByIpIdIn(dsUrns));
        }
        Set<UniformResourceName> otherUrns = Arrays.stream(ipIds)
                                                   .filter(ipId -> ipId.getEntityType() != EntityType.DATASET)
                                                   .collect(Collectors.toSet());
        if (!otherUrns.isEmpty()) {
            entities.addAll(entityRepository.findByIpIdIn(otherUrns));
        }
        return entities;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IComputedAttribute<Dataset, ?>> Set<T> getComputationPlugins(Dataset dataset) {

        Set<ModelAttrAssoc> computedAttributes = modelAttributeService.getComputedAttributes(dataset.getModel()
                                                                                                    .getId());
        Set<T> computationPlugins = new HashSet<>();
        try {
            for (ModelAttrAssoc attr : computedAttributes) {
                try {
                    IPluginParam resultFragmentName = IPluginParam.build(IComputedAttribute.RESULT_FRAGMENT_NAME,
                                                                         attr.getAttribute().getFragment().getName())
                                                                  .dynamic();
                    IPluginParam resultAttrName = IPluginParam.build(IComputedAttribute.RESULT_ATTRIBUTE_NAME,
                                                                     attr.getAttribute().getName()).dynamic();
                    IComputedAttribute<?, ?> plugin = pluginService.getPlugin(attr.getComputationConf().getBusinessId(),
                                                                              resultAttrName,
                                                                              resultFragmentName);
                    // here we have a plugin with no idea of the type of the generic parameter used by the "compute"
                    // method, lets check that it is a IComputedAttribute<Dataset,?>
                    plugin.getClass().getMethod("compute", Dataset.class);
                    // if no exception has been thrown then the method exist and we are in presence of a
                    // IComputedAttribute<Dataset, ?>
                    computationPlugins.add((T) plugin);
                } catch (NoSuchMethodException e) {
                    // this is a normal exception in the logic of the method: to know if we have an
                    // IComputedAttribute<Dataset, ?> we check if a method compute(Dataset) is defined, if not then we
                    // just don't consider this plugin
                } catch (NotAvailablePluginConfigurationException e) {
                    LOGGER.warn(
                        "Unable to compute dataset attribute value cause IComputedAttribute plugin is not avtive.",
                        e);
                }
            }
        } catch (ModuleException e) {
            // Rethrow as a runtime because anyway there is nothing we can do there, if the plugin cannot be
            // instantiated the system should set itself to maintenance mode!
            throw new RsRuntimeException(e);
        } catch (SecurityException e) {
            // This exception should not happen. so if it does lets put the system into maintenance mode
            throw new RsRuntimeException(e);
        }
        return computationPlugins;
    }

}
