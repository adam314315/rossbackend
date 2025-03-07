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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.dam.dao.entities.*;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import fr.cnes.regards.modules.dam.domain.entities.*;
import fr.cnes.regards.modules.dam.domain.entities.event.BroadcastEntityEvent;
import fr.cnes.regards.modules.dam.domain.entities.event.DatasetEvent;
import fr.cnes.regards.modules.dam.domain.entities.event.EventType;
import fr.cnes.regards.modules.dam.domain.entities.event.NotDatasetEntityEvent;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.dam.service.entities.exception.InvalidFileLocation;
import fr.cnes.regards.modules.dam.service.entities.validation.AbstractEntityValidationService;
import fr.cnes.regards.modules.dam.service.settings.IDamSettingsService;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.dto.properties.AbstractProperty;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.dto.properties.ObjectProperty;
import fr.cnes.regards.modules.model.service.IModelService;
import fr.cnes.regards.modules.model.service.validation.IModelFinder;
import fr.cnes.regards.modules.model.service.validation.ValidationMode;
import fr.cnes.regards.modules.model.service.validation.validator.common.NotAlterableAttributeValidator;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract parameterized entity service
 *
 * @param <U> Entity type
 * @author oroussel
 */
public abstract class AbstractEntityService<F extends EntityFeature, U extends AbstractEntity<F>>
    extends AbstractEntityValidationService<F, U> implements IEntityService<U> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntityService.class);

    private static final String CATALOG_DOWNLOAD_PATH = "/downloads/{aip_id}/files/{checksum}";

    public static final String UNABLE_TO_ACCESS_STORAGE_PLUGIN = "Unable to access storage plugin";

    /**
     * Map of {@link Project}s by tenant
     */
    private final Map<String, Project> projects = new HashMap<>();

    /**
     * {@link IModelService} instance
     */
    protected final IModelService modelService;

    @Autowired
    private ILocalStorageService localStorageService;

    @Autowired
    private IProjectsClient projectClient;

    @Autowired
    private INotificationClient notificationClient;

    private final IPluginService pluginService;

    protected final IDamSettingsService damSettingsService;

    /**
     * Parameterized entity repository
     */
    protected final IAbstractEntityRepository<U> abstractEntityRepository;

    /**
     * Unparameterized entity repository
     */
    protected final IAbstractEntityRepository<AbstractEntity<?>> entityRepository;

    /**
     * Collection repository
     */
    protected final ICollectionRepository collectionRepository;

    /**
     * Dataset repository
     */
    protected final IDatasetRepository datasetRepository;

    private final IDeletedEntityRepository deletedEntityRepository;

    private final EntityManager em;

    /**
     * {@link IPublisher} instance
     */
    private final IPublisher publisher;

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * The plugin's class name of type {@link IStorageService} used to store AIP entities
     */
    @Value("${regards.dam.store.files.plugin:fr.cnes.regards.modules.dam.service.entities.plugins.StoragePlugin}")
    private String storeEntityFilesPlugin;

    @Value("${regards.dam.attachment.inputs.path:/dataset-attachments-input}")
    private String localInputPath;

    private PluginConfiguration storeEntityPluginConf;

    @Value("${prefix.path}")
    private String urlPrefix;

    private final IAbstractEntityRequestRepository abstractEntityRequestRepo;

    protected AbstractEntityService(IModelFinder modelFinder,
                                    IAbstractEntityRepository<AbstractEntity<?>> entityRepository,
                                    IModelService modelService,
                                    IPluginService pluginService,
                                    IDamSettingsService damSettingsService,
                                    IDeletedEntityRepository deletedEntityRepository,
                                    ICollectionRepository collectionRepository,
                                    IDatasetRepository datasetRepository,
                                    IAbstractEntityRepository<U> abstractEntityRepository,
                                    EntityManager em,
                                    IPublisher publisher,
                                    IRuntimeTenantResolver runtimeTenantResolver,
                                    IAbstractEntityRequestRepository abstractEntityRequestRepo) {
        super(modelFinder);
        this.entityRepository = entityRepository;
        this.modelService = modelService;
        this.damSettingsService = damSettingsService;
        this.deletedEntityRepository = deletedEntityRepository;
        this.collectionRepository = collectionRepository;
        this.datasetRepository = datasetRepository;
        this.abstractEntityRepository = abstractEntityRepository;
        this.pluginService = pluginService;
        this.em = em;
        this.publisher = publisher;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.abstractEntityRequestRepo = abstractEntityRequestRepo;
    }

    @Override
    public U load(UniformResourceName ipId) throws ModuleException {
        U entity = abstractEntityRepository.findOneByIpId(ipId);
        if (entity == null) {
            throw new EntityNotFoundException(ipId.toString(), this.getClass());
        }
        return entity;
    }

    @Override
    public U load(Long id) throws ModuleException {
        Assert.notNull(id, "Entity identifier is required");
        Optional<U> entityOpt = abstractEntityRepository.findById(id);
        if (entityOpt.isEmpty()) {
            throw new EntityNotFoundException(id, this.getClass());
        }
        return entityOpt.get();
    }

    @Override
    public U loadWithRelations(UniformResourceName ipId) throws ModuleException {
        U entity = abstractEntityRepository.findByIpId(ipId);
        if (entity == null) {
            throw new EntityNotFoundException(ipId.toString(), this.getClass());
        }
        return entity;
    }

    @Override
    public List<U> loadAllWithRelations(UniformResourceName... ipIds) {
        return abstractEntityRepository.findByIpIdIn(ImmutableSet.copyOf(ipIds));
    }

    @Override
    public Page<U> findAll(Pageable pageRequest) {
        return abstractEntityRepository.findAll(pageRequest);
    }

    @Override
    public Set<U> findAllByProviderId(String providerId) {
        return abstractEntityRepository.findAllByProviderId(providerId);
    }

    @Override
    public Page<U> search(String label, Pageable pageRequest) {
        EntitySpecifications<U> entitySpecifications = new EntitySpecifications<>();
        return abstractEntityRepository.findAll(entitySpecifications.searchByAndOrderByLabel(label), pageRequest);
    }

    @Override
    public List<U> findAll() {
        return abstractEntityRepository.findAll();
    }

    /**
     * Check if model is loaded else load it then set it on entity.
     *
     * @param entity concerned entity
     */
    @Override
    public void checkAndOrSetModel(U entity) throws ModuleException {
        Model model = entity.getModel();
        // Load model by name if id not specified
        if ((model.getId() == null) && (model.getName() != null)) {
            model = modelService.getModelByName(model.getName());
            entity.setModel(model);
        }
    }

    /**
     * Compute available validators
     *
     * @param modelAttribute {@link ModelAttrAssoc}
     * @param attributeKey   attribute key
     * @param mode           manage update or not
     * @return {@link Validator} list
     */
    @Override
    protected List<Validator> getValidators(ModelAttrAssoc modelAttribute,
                                            String attributeKey,
                                            ValidationMode mode,
                                            F feature) {

        List<Validator> validators = super.getValidators(modelAttribute, attributeKey, mode, feature);

        AttributeModel attModel = modelAttribute.getAttribute();

        // Check alterable attribute
        // Update mode only :
        if (ValidationMode.UPDATE.equals(mode) && !attModel.isAlterable()) {
            // let's retrieve the value of the property from db and check if it's the same value.
            AbstractEntity<?> fromDb = entityRepository.findByIpId(feature.getId());
            IProperty<?> valueFromDb = extractProperty(fromDb.getFeature(), attModel);
            IProperty<?> valueFromEntity = extractProperty(feature, attModel);
            // retrieve entity from db, and then update the new one, but I do not have the entity here....
            validators.add(new NotAlterableAttributeValidator(attributeKey, attModel, valueFromDb, valueFromEntity));
        }
        return validators;
    }

    protected IProperty<?> extractProperty(EntityFeature feature, AttributeModel attribute) {
        Fragment fragment = attribute.getFragment();
        String attName = attribute.getName();
        String attPath = fragment.isDefaultFragment() ? attName : fragment.getName() + "." + attName;
        return feature.getProperty(attPath);
    }

    /**
     * Build real attribute map extracting namespace from {@link ObjectProperty} (i.e. fragment name)
     *
     * @param attMap     Map to build
     * @param namespace  namespace context
     * @param attributes {@link AbstractProperty} list to analyze
     */
    protected void buildAttributeMap(Map<String, IProperty<?>> attMap, String namespace, Set<IProperty<?>> attributes) {
        if (attributes != null) {
            for (IProperty<?> att : attributes) {
                // Compute value
                if (ObjectProperty.class.equals(att.getClass())) {
                    ObjectProperty o = (ObjectProperty) att;
                    buildAttributeMap(attMap, att.getName(), o.getValue());
                } else {
                    // Compute key
                    String key = att.getName();
                    if (!namespace.equals(Fragment.getDefaultName())) {
                        key = namespace.concat(".").concat(key);
                    }
                    LOGGER.debug("Key \"{}\" -> \"{}\".", key, att);
                    attMap.put(key, att);
                }
            }
        }
    }

    /**
     * Associate a list of {@link String} tags to the given existing entity.
     *
     * @param entityId an AbstractEntity identifier
     * @param tagList  UniformResourceName Set representing AbstractEntity to be associated to pCollection
     */
    @Override
    public void associate(Long entityId, Set<String> tagList) throws ModuleException {
        Optional<U> entityOpt = abstractEntityRepository.findById(entityId);
        if (entityOpt.isEmpty()) {
            throw new EntityNotFoundException(entityId, this.getClass());
        }
        // Adding new tags to detached entity
        U entity = entityOpt.get();
        em.detach(entity);
        tagList.forEach(entity::addTags);
        U entityInDb = abstractEntityRepository.findById(entityId).get();
        // And detach it because it is the other one that will be persisted
        em.detach(entityInDb);
        this.updateWithoutCheck(entity, entityInDb);
    }

    @Override
    public U create(U inEntity) throws ModuleException {
        U entity = checkCreation(inEntity);

        // Set IpId
        if (entity.getIpId() == null) {
            entity.setIpId(new OaisUniformResourceName(OAISIdentifier.AIP,
                                                       EntityType.valueOf(entity.getType()),
                                                       runtimeTenantResolver.getTenant(),
                                                       UUID.randomUUID(),
                                                       1,
                                                       null,
                                                       null));
        }

        // As long as there is no way to create new entity version thanks to dam,
        // we set last flag and virtualId unconditionally
        entity.setLast(true);
        entity.setVirtualId();

        // IpIds of entities that will need an AMQP event publishing
        Set<UniformResourceName> updatedIpIds = new HashSet<>();
        entity.setCreationDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC));
        this.manageGroups(entity, updatedIpIds);

        if (damSettingsService.isStoreFiles()) {
            // call storage
            try {
                IStorageService storageService = getStorageService();

                if (storageService != null) {
                    storageService.store(entity);
                } else {
                    LOGGER.warn(UNABLE_TO_ACCESS_STORAGE_PLUGIN);
                }
            } catch (NotAvailablePluginConfigurationException e) {
                LOGGER.warn(UNABLE_TO_ACCESS_STORAGE_PLUGIN, e);
            }
        } else {
            setDataFilesUri(entity);
        }

        entity = abstractEntityRepository.save(entity);
        updatedIpIds.add(entity.getIpId());

        // AMQP event publishing
        publishEvents(EventType.CREATE, updatedIpIds);
        return entity;
    }

    @Override
    public void dissociate(Long entityId, Set<String> ipIds) throws ModuleException {
        Optional<U> entityOpt = abstractEntityRepository.findById(entityId);
        if (entityOpt.isEmpty()) {
            throw new EntityNotFoundException(entityId, this.getClass());
        }
        U entity = entityOpt.get();
        // Removing tags to detached entity
        em.detach(entity);
        entity.removeTags(ipIds);
        U entityInDb = abstractEntityRepository.findById(entityId).get();
        // And detach it too because it is the other one that will be persisted
        em.detach(entityInDb);
        this.updateWithoutCheck(entity, entityInDb);
    }

    @Override
    public void publishEvents(EventType eventType, Set<UniformResourceName> ipIds) {
        UniformResourceName[] datasetsIpIds = ipIds.stream()
                                                   .filter(ipId -> ipId.getEntityType() == EntityType.DATASET)
                                                   .toArray(UniformResourceName[]::new);
        if (datasetsIpIds.length > 0) {
            publisher.publish(new DatasetEvent(datasetsIpIds));
        }
        UniformResourceName[] notDatasetsIpIds = ipIds.stream()
                                                      .filter(ipId -> ipId.getEntityType() != EntityType.DATASET)
                                                      .toArray(UniformResourceName[]::new);
        if (notDatasetsIpIds.length > 0) {
            publisher.publish(new NotDatasetEntityEvent(notDatasetsIpIds));
        }
        publisher.publish(new BroadcastEntityEvent(eventType, ipIds.toArray(new UniformResourceName[0])));
    }

    /**
     * If entity is a collection or a dataset, recursively follow tags to add entity groups, then, if entity is a
     * collection, retrieve and add all groups from collections and datasets tagging this entity
     *
     * @param entity entity to manage the add of groups
     */
    private <T extends AbstractEntity<?>> void manageGroups(T entity, Set<UniformResourceName> updatedIpIds) {
        // Search Datasets and collections which tag this entity (if entity is a collection)
        if (entity instanceof Collection) {
            List<AbstractEntity<?>> taggingEntities = entityRepository.findByTags(entity.getIpId().toString());
            for (AbstractEntity<?> e : taggingEntities) {
                if ((e instanceof Dataset) || (e instanceof Collection)) {
                    entity.getGroups().addAll(e.getGroups());
                }
            }
        }

        // If entity is a collection or a dataset => propagate its groups to tagged collections (recursively)
        if (((entity instanceof Collection) || (entity instanceof Dataset)) && !entity.getTags().isEmpty()) {
            List<AbstractEntity<?>> taggedColls = entityRepository.findByIpIdIn(extractUrnsOfType(entity.getTags(),
                                                                                                  EntityType.COLLECTION));
            for (AbstractEntity<?> coll : taggedColls) {
                if (coll.getGroups().addAll(entity.getGroups())) {
                    // If collection has already been updated, stop recursion !!! (else StackOverflow)
                    updatedIpIds.add(coll.getIpId());
                    this.manageGroups(coll, updatedIpIds);
                }
            }
        }
        entityRepository.save(entity);
    }

    private U checkCreation(U pEntity) throws ModuleException {
        checkModelExists(pEntity);
        doCheck(pEntity, null);
        return pEntity;
    }

    /**
     * Specific check depending on entity type
     */
    protected void doCheck(U pEntity, U entityInDB) throws ModuleException {
        // nothing by default
    }

    /**
     * checks if the entity requested exists and that it is modified according to one of its former version( pEntity's
     * id is pEntityId)
     *
     * @return current entity
     * @throws ModuleException thrown if the entity cannot be found or if entities' id do not match
     */
    private U checkUpdate(Long entityId, U entity) throws ModuleException {
        // Detach new entity if already attached to properly load database one
        if (em.contains(entity)) {
            em.detach(entity);
        }
        Optional<U> entityInDbOpt = abstractEntityRepository.findById(entityId);
        if (entityInDbOpt.isEmpty()) {
            throw new EntityNotFoundException(entityId, this.getClass());
        }
        U entityInDb = entityInDbOpt.get();
        em.detach(entityInDb);
        if (!entityId.equals(entity.getId())) {
            throw new EntityInconsistentIdentifierException(entityId, entity.getId(), entity.getClass());
        }
        doCheck(entity, entityInDb);
        return entityInDb;
    }

    @Override
    public U update(Long pEntityId, U pEntity) throws ModuleException {
        // checks
        U entityInDb = checkUpdate(pEntityId, pEntity);
        return updateWithoutCheck(pEntity, entityInDb);
    }

    @Override
    public U update(UniformResourceName entityUrn, U entity) throws ModuleException {
        U entityInDb = abstractEntityRepository.findOneByIpId(entityUrn);
        if (entityInDb == null) {
            throw new EntityNotFoundException(entity.getIpId().toString());
        }
        entity.setId(entityInDb.getId());
        // checks
        entityInDb = checkUpdate(entityInDb.getId(), entity);
        return updateWithoutCheck(entity, entityInDb);
    }

    /**
     * Really do the update of entities
     *
     * @param entity     updated entity to be saved
     * @param entityInDb only there for comparison for group management
     * @return updated entity with group set correctly
     */
    private U updateWithoutCheck(U entity, U entityInDb) throws ModuleException {
        Set<UniformResourceName> oldLinks = extractUrns(entityInDb.getTags());
        Set<UniformResourceName> newLinks = extractUrns(entity.getTags());
        Set<String> oldGroups = entityInDb.getGroups();
        Set<String> newGroups = entity.getGroups();
        // IpId URNs of updated entities (those which need an AMQP event publish)
        Set<UniformResourceName> updatedIpIds = new HashSet<>();
        // Update entity, checks already assures us that everything which is updated can be updated, so we can just put
        // pEntity into the DB.
        entity.setLastUpdate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC));

        U updated = abstractEntityRepository.save(entity);

        updatedIpIds.add(updated.getIpId());
        // Compute tags to remove and tags to add
        if (!oldLinks.equals(newLinks) || !oldGroups.equals(newGroups)) {
            Set<UniformResourceName> tagsToRemove = getDiff(oldLinks, newLinks);
            // For all previously tagged entities, retrieve all groups...
            Set<String> groupsToRemove = new HashSet<>();
            List<AbstractEntity<?>> taggedEntitiesWithGroupsToRemove = entityRepository.findByIpIdIn(tagsToRemove);
            taggedEntitiesWithGroupsToRemove.forEach(e -> groupsToRemove.addAll(e.getGroups()));
            // ... delete all these groups on all collections...
            for (String group : groupsToRemove) {
                List<Collection> collectionsWithGroup = collectionRepository.findByGroups(group);
                collectionsWithGroup.forEach(c -> c.getGroups().remove(group));
                collectionRepository.saveAll(collectionsWithGroup);
                // Add collections to IpIds to be published on AMQP
                collectionsWithGroup.forEach(c -> updatedIpIds.add(c.getIpId()));
                // ... then manage concerned groups on all datasets containing them
                List<Dataset> datasetsWithGroup = datasetRepository.findByGroups(group);
                datasetsWithGroup.forEach(ds -> this.manageGroups(ds, updatedIpIds));
                datasetRepository.saveAll(datasetsWithGroup);
                // Add datasets to IpIds to be published on AMQP
                datasetsWithGroup.forEach(ds -> updatedIpIds.add(ds.getIpId()));
            }
            // Don't forget to manage groups for current entity too
            this.manageGroups(updated, updatedIpIds);
        }

        if (damSettingsService.isStoreFiles()) {
            // call storage
            try {
                IStorageService storageService = getStorageService();
                if (storageService != null) {
                    storageService.update(updated, entityInDb);
                } else {
                    LOGGER.warn(UNABLE_TO_ACCESS_STORAGE_PLUGIN);
                }
            } catch (NotAvailablePluginConfigurationException e) {
                LOGGER.warn(UNABLE_TO_ACCESS_STORAGE_PLUGIN, e);
            }
        } else {
            setDataFilesUri(updated, entityInDb);
            abstractEntityRepository.save(updated);
        }

        // AMQP event publishing
        publishEvents(EventType.UPDATE, updatedIpIds);
        return updated;
    }

    @Override
    public U save(U entity) {
        return abstractEntityRepository.save(entity);
    }

    @Override
    public U delete(Long pEntityId) throws ModuleException {
        Assert.notNull(pEntityId, "Entity identifier is required");
        U toDelete = load(pEntityId);
        return delete(toDelete);
    }

    protected U delete(U toDelete) throws ModuleException {
        UniformResourceName urn = toDelete.getIpId();
        // IpId URNs that will need an AMQP event publishing
        Set<UniformResourceName> updatedIpIds = new HashSet<>();
        // Manage tags (must be done before group managing to avoid bad propagation)
        // Retrieve all entities tagging the one to delete
        List<AbstractEntity<?>> taggingEntities = entityRepository.findByTags(urn.toString());
        // Manage tags
        for (AbstractEntity<?> taggingEntity : taggingEntities) {
            // remove tag to ipId
            taggingEntity.removeTags(Collections.singletonList(urn.toString()));
        }
        // Save all these tagging entities
        entityRepository.saveAll(taggingEntities);
        taggingEntities.forEach(e -> updatedIpIds.add(e.getIpId()));

        // datasets that contain one of the entity groups
        Set<Dataset> datasets = new HashSet<>();
        // If entity contains groups => update all entities tagging this entity (recursively)
        // Need to manage groups one by one
        for (String group : toDelete.getGroups()) {
            // Find all collections containing group.
            List<Collection> collectionsWithGroup = collectionRepository.findByGroups(group);
            // Remove group from collections groups
            collectionsWithGroup.stream().filter(c -> !c.equals(toDelete)).forEach(c -> c.getGroups().remove(group));
            // Find all datasets containing this group (to rebuild groups propagation later)
            datasets.addAll(datasetRepository.findByGroups(group));
        }
        // Remove dataset to delete from datasets (no need to manage its groups)
        datasets.remove(toDelete);
        // Remove relate files
        for (Map.Entry<DataType, DataFile> entry : toDelete.getFiles().entries()) {
            if ((entry != null) && (entry.getValue() != null) && localStorageService.isFileLocallyStored(toDelete,
                                                                                                         entry.getValue())) {
                localStorageService.removeFile(toDelete, entry.getValue());
            }
        }
        // Delete the entity
        entityRepository.delete(toDelete);
        updatedIpIds.add(toDelete.getIpId());
        // Manage all impacted datasets groups from scratch
        datasets.forEach(ds -> this.manageGroups(ds, updatedIpIds));

        try {
            deleteAipStorage(toDelete);
        } catch (NotAvailablePluginConfigurationException e1) {
            LOGGER.warn("Enabled to delete AIP storage cause storage plugin is not active", e1);
        }

        deletedEntityRepository.save(createDeletedEntity(toDelete));

        // Publish events to AMQP
        publishEvents(EventType.DELETE, updatedIpIds);

        return toDelete;
    }

    /**
     * @param pSource Set of UniformResourceName
     * @param pOther  Set of UniformResourceName to remove from pSource
     * @return a new Set of UniformResourceName containing only the elements present into pSource and not in pOther
     */
    private Set<UniformResourceName> getDiff(Set<UniformResourceName> pSource, Set<UniformResourceName> pOther) {
        Set<UniformResourceName> result = new HashSet<>(pSource);
        result.removeAll(pOther);
        return result;
    }

    public void checkModelExists(AbstractEntity<?> entity) throws ModuleException {
        // model must exist : EntityNotFoundException thrown if not
        modelService.getModel(entity.getModel().getId());
    }

    private static Set<UniformResourceName> extractUrns(Set<String> tags) {
        return tags.stream()
                   .filter(OaisUniformResourceName::isValidUrn)
                   .map(OaisUniformResourceName::fromString)
                   .collect(Collectors.toSet());
    }

    private static Set<UniformResourceName> extractUrnsOfType(Set<String> tags, EntityType entityType) {
        return tags.stream()
                   .filter(OaisUniformResourceName::isValidUrn)
                   .map(OaisUniformResourceName::fromString)
                   .filter(urn -> urn.getEntityType() == entityType)
                   .collect(Collectors.toSet());
    }

    private static DeletedEntity createDeletedEntity(AbstractEntity<?> entity) {
        DeletedEntity delEntity = new DeletedEntity();
        delEntity.setCreationDate(entity.getCreationDate());
        delEntity.setDeletionDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC));
        delEntity.setIpId(entity.getIpId());
        delEntity.setLastUpdate(entity.getLastUpdate());
        return delEntity;
    }

    @Override
    public U attachFiles(UniformResourceName urn,
                         DataType dataType,
                         MultipartFile[] attachments,
                         List<DataFile> refs,
                         String fileUriTemplate) throws ModuleException {
        validateRefs(refs);
        U entity = loadWithRelations(urn);
        // Store files locally
        java.util.Collection<DataFile> files = localStorageService.attachFiles(entity,
                                                                               dataType,
                                                                               attachments,
                                                                               fileUriTemplate);
        // Merge previous files with new ones
        if (entity.getFiles().get(dataType) != null) {
            entity.getFiles().get(dataType).addAll(files);
        } else {
            entity.getFiles().putAll(dataType, files);
        }

        // Merge references
        attachRefs(dataType, refs, entity, fileUriTemplate);

        return update(entity);
    }

    private void attachRefs(DataType dataType, List<DataFile> refs, U entity, String fileUriTemplate)
        throws ModuleException {
        List<DataFile> localFileToAttach = new ArrayList<>();
        for (DataFile ref : refs) {
            // Same logic as for normal file is applied to check format support
            ContentTypeValidator.supports(dataType, ref.getFilename(), ref.getMimeType().toString());
            if (ref.getUri().startsWith("file:")) {
                localFileToAttach.add(ref);
            } else {
                // Compute checksum on URI for removal
                try {
                    ref.setChecksum(ChecksumUtils.computeHexChecksum(ref.getUri(),
                                                                     LocalStorageService.DIGEST_ALGORITHM));
                    ref.setDigestAlgorithm(LocalStorageService.DIGEST_ALGORITHM);
                } catch (NoSuchAlgorithmException | IOException e) {
                    String message = "Error while computing checksum";
                    LOGGER.error(message, e);
                    throw new ModuleException(message, e);
                }
                if (entity.getFiles().get(dataType) != null) {
                    entity.getFiles().get(dataType).add(ref);
                } else {
                    entity.getFiles().put(dataType, ref);
                }
            }
        }
        if (!localFileToAttach.isEmpty()) {
            java.util.Collection<DataFile> files = localStorageService.attachLocalFiles(entity,
                                                                                        dataType,
                                                                                        localFileToAttach,
                                                                                        fileUriTemplate);
            entity.getFiles().get(dataType).addAll(files);
        }
    }

    /**
     * Validate each DataFile url:
     * <ul>
     *     <li>Url is valid</li>
     *     <li>Url must be protocol file, http or https</li>
     *     <li>If protocol file : url root must be the configured one in dataprovider (localInputPath config)</li>
     * </ul>
     *
     * @throws ModuleException if any rule of any DataFile isn't respected
     */
    private void validateRefs(@Nullable List<DataFile> refs) throws ModuleException {
        if (refs == null) {
            throw new ModuleException("Refs must not be null");
        }
        try {
            for (DataFile ref : refs) {
                URL url = new URL(ref.getUri());
                String uriProtocol = url.getProtocol();
                if (uriProtocol.equals("file")) {
                    if (!url.getPath().startsWith(localInputPath)) {
                        throw new ModuleException("Not authorized path. It must be inside dam attachment input "
                                                  + "directory (starts with 'file:"
                                                  + localInputPath
                                                  + "')");
                    }
                } else if (!uriProtocol.startsWith("http")) {
                    // only protocols "file", "http" and "https" are authorized
                    throw new ModuleException("Forbidden URL protocol '" + uriProtocol + "'");
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ModuleException(e);
        }
    }

    @Override
    public DataFile getFile(UniformResourceName urn, String checksum) throws ModuleException {

        U entity = load(urn);
        // Search data file
        Multimap<DataType, DataFile> files = entity.getFiles();
        for (Map.Entry<DataType, DataFile> entry : files.entries()) {
            if (checksum.equals(entry.getValue().getChecksum())) {
                return entry.getValue();
            }
        }

        String message = String.format("Data file with checksum \"%s\" in entity \"%s\" not found",
                                       checksum,
                                       urn.toString());
        LOGGER.error(message);
        throw new EntityNotFoundException(message);
    }

    @Override
    public void downloadFile(UniformResourceName urn, String checksum, OutputStream output) throws ModuleException {

        U entity = load(urn);
        // Retrieve data file
        DataFile dataFile = getFile(urn, checksum);
        if (localStorageService.isFileLocallyStored(entity, dataFile)) {
            localStorageService.getFileContent(checksum, output);
        } else {
            throw new InvalidFileLocation(dataFile.getFilename());
        }
    }

    @Override
    public U removeFile(UniformResourceName urn, String checksum) throws ModuleException {

        U entity = load(urn);
        // Retrieve data file
        DataFile dataFile = getFile(urn, checksum);
        // Try to remove the file if locally stored, otherwise the file is not stored on this microservice
        if (localStorageService.isFileLocallyStored(entity, dataFile)) {
            localStorageService.removeFile(entity, dataFile);
        }
        entity.getFiles().get(dataFile.getDataType()).remove(dataFile);
        return update(entity);
    }

    /**
     * Initiate new plugin configuration for {@link IStorageService} used to store entities associated files.
     * The used plugin is defined by the plugin class name set in the storeEntityFilesPlugin property.
     *
     * @throws NotAvailablePluginConfigurationException Plugin not available or not found.
     */
    private void initStoragePluginConfiguration() throws NotAvailablePluginConfigurationException {
        try {
            storeEntityPluginConf = PluginConfiguration.build(Class.forName(storeEntityFilesPlugin),
                                                              null,
                                                              IPluginParam.set());
            storeEntityPluginConf.setMetaData(PluginUtils.getPlugins().get(storeEntityPluginConf.getPluginId()));
            storeEntityPluginConf.setVersion(storeEntityPluginConf.getMetaData().getVersion());
            pluginService.savePluginConfiguration(storeEntityPluginConf);
        } catch (ClassNotFoundException | EntityInvalidException | EncryptionException | EntityNotFoundException e) {
            throw new NotAvailablePluginConfigurationException(e.getMessage(), e);
        }
    }

    /**
     * @return a {@link Plugin} implementation of {@link IStorageService}
     */
    private IStorageService getStorageService() throws NotAvailablePluginConfigurationException {
        if (storeEntityPluginConf == null) {
            initStoragePluginConfiguration();
        }
        try {
            return pluginService.getPlugin(storeEntityPluginConf);
        } catch (ModuleException e) {
            throw new NotAvailablePluginConfigurationException(e.getMessage(), e);
        }
    }

    private void deleteAipStorage(U entity) throws NotAvailablePluginConfigurationException {
        if (damSettingsService.isStoreFiles()) {
            getStorageService().delete(entity);
        }
    }

    @Override
    public void storeSucces(Set<RequestInfo> requests) {
        Set<AbstractEntityRequest> succesRequests = this.abstractEntityRequestRepo.findByGroupIdIn(requests.stream()
                                                                                                           .map(
                                                                                                               RequestInfo::getGroupId)
                                                                                                           .collect(
                                                                                                               Collectors.toSet()));
        Map<UniformResourceName, AbstractEntity<? extends EntityFeature>> entityByUrn = this.entityRepository.findByIpIdIn(
                                                                                                succesRequests.stream().map(AbstractEntityRequest::getUrn).collect(Collectors.toSet()))
                                                                                                             .stream()
                                                                                                             .collect(
                                                                                                                 Collectors.toMap(
                                                                                                                     AbstractEntity::getIpId,
                                                                                                                     Function.identity()));
        boolean updated = false;
        Set<AbstractEntityRequest> treatedRequests = new HashSet<>();
        // for all request succeeded
        for (RequestInfo info : requests) {
            // get the AbstractEntityRequest with a matching groupId
            Optional<AbstractEntityRequest> oCurrent = succesRequests.stream()
                                                                     .filter(matchingRequest -> matchingRequest.getGroupId()
                                                                                                               .equals(
                                                                                                                   info.getGroupId()))
                                                                     .findAny();
            if (oCurrent.isPresent()) {
                AbstractEntityRequest current = oCurrent.get();
                for (RequestResultInfoDto request : info.getSuccessRequests()) {
                    AbstractEntity<? extends EntityFeature> entity = entityByUrn.get(current.getUrn());
                    // if we found a AbstractEntity matching with the urn stored in the  AbstractEntityRequest
                    if (entity != null) {
                        // seek the file inside the AbstractEntity with the checksum matching with it inside the
                        // RequestResultInfoDto
                        for (DataFile file : entity.getFiles().values()) {
                            // if the file is found we update it uri
                            if (file.getChecksum().equals(request.getResultFile().getMetaInfo().getChecksum())) {
                                try {
                                    file.setUri(getDownloadUrl(current.getUrn(),
                                                               file.getChecksum(),
                                                               runtimeTenantResolver.getTenant(),
                                                               false));
                                    treatedRequests.add(current);
                                    updated = true;
                                } catch (ModuleException e) {
                                    LOGGER.error("Cannot get download url for data file : " + file.getFilename(), e);
                                }
                                // remove buffered file
                                try {
                                    localStorageService.removeFile(entity, file);
                                } catch (ModuleException e) {
                                    LOGGER.trace("Cannot remove file " + file.getFilename() + "stored locally in DAM",
                                                 e);
                                }
                            }
                        }
                    } else {
                        LOGGER.error("No feature found with urn {}", current.getUrn());
                    }
                }
                if (!updated) {
                    LOGGER.error("Request with group id {} has not been updated", current.getGroupId());
                }
            } else {
                LOGGER.trace("Request with group id {} does not belong to this microservice.", info.getGroupId());
            }
        }

        // delete treated requests
        this.abstractEntityRequestRepo.deleteAll(treatedRequests);
        this.entityRepository.saveAll(entityByUrn.values());
        this.publishEvents(EventType.UPDATE,
                           treatedRequests.stream().map(AbstractEntityRequest::getUrn).collect(Collectors.toSet()));
    }

    @Override
    public void storeError(Set<RequestInfo> requests) {
        StringBuilder buf = new StringBuilder("Storage failed. Errors :<ul>");
        Set<AbstractEntityRequest> treatedRequests = new HashSet<>();
        for (RequestInfo request : requests) {
            // Check if request is a known one
            Optional<AbstractEntityRequest> oReq = this.abstractEntityRequestRepo.findByGroupId(request.getGroupId());
            if (oReq.isPresent()) {
                treatedRequests.add(oReq.get());
                Set<String> errors = request.getErrorRequests()
                                            .stream()
                                            .map(RequestResultInfoDto::getErrorCause)
                                            .collect(Collectors.toSet());
                for (String error : errors) {
                    buf.append(String.format("<li>%s</li>", error));
                }
                LOGGER.error("Storage request with groupId {} failed", request.getGroupId());
            }
        }
        if (!treatedRequests.isEmpty()) {
            buf.append("</ul>");
            this.notificationClient.notify(buf.toString(),
                                           "Data-management storage failed",
                                           NotificationLevel.ERROR,
                                           MimeTypeUtils.TEXT_HTML,
                                           DefaultRole.PROJECT_ADMIN);
            // delete treated requests
            this.abstractEntityRequestRepo.deleteAll(treatedRequests);
        }
    }

    /**
     * Generate URL to access file from REGARDS system thanks to is checksum
     */
    private String getDownloadUrl(UniformResourceName uniformResourceName,
                                  String checksum,
                                  String tenant,
                                  boolean locallyStored) throws ModuleException {
        Project project = projects.get(tenant);
        if (project == null) {
            FeignSecurityManager.asSystem();
            project = ResponseEntityUtils.extractContentOrThrow(projectClient.retrieveProject(tenant),
                                                                "Error while retrieving project : response body is empty");
            projects.put(tenant, project);
            FeignSecurityManager.reset();
        }
        String proxyfiedUrl = project.getHost() //NOSONAR -> impossible NPE, managed in a condition 5 lines upper
                              + urlPrefix + "/" + encode4Uri("rs-catalog") + CATALOG_DOWNLOAD_PATH.replace("{aip_id}",
                                                                                                           uniformResourceName.toString())
                                                                                                  .replace("{checksum}",
                                                                                                           checksum);
        if (locallyStored) {
            proxyfiedUrl += "/dam";
        }
        return proxyfiedUrl;
    }

    private void setDataFilesUri(U entity, U entityInDb) throws ModuleException {
        String tenant = runtimeTenantResolver.getTenant();
        Stream<DataFile> newDataFilesStream = entity.getFiles().values().stream();
        // set uri only for new files
        if (entityInDb != null) {
            // compare previous list and new list to get new files
            newDataFilesStream = newDataFilesStream.filter(file -> !entityInDb.getFiles().values().contains(file));
        }
        // don't modify external uri (!isReference)
        List<DataFile> newDataFiles = newDataFilesStream.filter(dataFile -> Boolean.FALSE.equals(dataFile.isReference()))
                                                        .toList();
        for (DataFile dataFile : newDataFiles) {
            dataFile.setUri(getDownloadUrl(entity.getIpId(), dataFile.getChecksum(), tenant, true));
        }
    }

    private void setDataFilesUri(U entity) throws ModuleException {
        setDataFilesUri(entity, null);
    }

    private static String encode4Uri(String str) {
        return new String(UriUtils.encode(str, Charset.defaultCharset().name()).getBytes(), StandardCharsets.US_ASCII);
    }
}
