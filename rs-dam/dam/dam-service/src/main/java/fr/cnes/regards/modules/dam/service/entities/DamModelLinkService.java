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

import fr.cnes.regards.modules.dam.dao.entities.ICollectionRepository;
import fr.cnes.regards.modules.dam.dao.entities.IDatasetRepository;
import fr.cnes.regards.modules.model.dao.IModelRepository;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.service.IModelLinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Marc SORDI
 */
@Service
public class DamModelLinkService implements IModelLinkService {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(DamModelLinkService.class);

    @Autowired
    private IDatasetRepository datasetRepository;

    @Autowired
    private ICollectionRepository collectionRepository;

    @Autowired
    private IModelRepository modelRepository;

    @Override
    public boolean isAttributeDeletable(Set<String> modelNames) {
        Set<Long> modelIds = modelNames.stream()
                                       .map(modelName -> modelRepository.findByName(modelName))
                                       .filter(Objects::nonNull)
                                       .map(Model::getId)
                                       .collect(Collectors.toSet());

        return !datasetRepository.isLinkedToEntities(modelIds)
               && !collectionRepository.isLinkedToEntities(modelIds)
               && !datasetRepository.isLinkedToDatasetsAsDataModel(modelNames);
    }

}
