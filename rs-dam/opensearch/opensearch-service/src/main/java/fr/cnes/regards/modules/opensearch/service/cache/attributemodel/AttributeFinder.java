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
package fr.cnes.regards.modules.opensearch.service.cache.attributemodel;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.domain.event.AttributeModelCreated;
import fr.cnes.regards.modules.model.domain.event.AttributeModelDeleted;
import fr.cnes.regards.modules.model.domain.event.AttributeModelUpdated;
import fr.cnes.regards.modules.model.dto.event.AttributeCacheRefreshEvent;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.model.gson.IAttributeHelper;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implement {@link IAttributeFinder}.
 *
 * @author Marc Sordi
 */
@Service
public class AttributeFinder implements IAttributeFinder, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeFinder.class);

    /**
     * Feign client for rs-dam {@link AttributeModel} controller. Autowired by Spring.
     */
    private final IAttributeHelper attributeHelper;

    private final ISubscriber subscriber;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Store dynamic and static properties by tenant. <br/>
     * Allows intelligent guess of attribute from a partial or complete JSON path preventing potential conflicts!<br/>
     */
    private final ConcurrentMap<String, Map<String, AttributeModel>> propertyMap = new ConcurrentHashMap<>();

    /**
     * Store dynamic and static properties by tenant and type for full text search
     */
    private final ConcurrentMap<String, Multimap<PropertyType, AttributeModel>> typedPropertyMap = new ConcurrentHashMap<>();

    public AttributeFinder(IAttributeHelper attributeModelClient,
                           ISubscriber subscriber,
                           IRuntimeTenantResolver runtimeTenantResolver) {
        this.attributeHelper = attributeModelClient;
        this.subscriber = subscriber;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public AttributeModel findByName(String name) throws OpenSearchUnknownParameter {

        AttributeModel attModel = getTenantMap().get(name);

        if (attModel == null) {
            String errorMessage = String.format("Unknown parameter %s for tenant %s",
                                                name,
                                                runtimeTenantResolver.getTenant());
            LOGGER.info(errorMessage);
            throw new OpenSearchUnknownParameter(errorMessage);
        }

        return attModel;
    }

    @Override
    public Optional<AttributeModel> findByNameOptional(String name) {
        return Optional.ofNullable(getTenantMap().get(name));
    }

    @Override
    public Set<AttributeModel> findByType(PropertyType type) throws OpenSearchUnknownParameter {

        Collection<AttributeModel> ppties = getTenantTypedMap().get(type);
        if (ppties == null) {
            String errorMessage = String.format("No parameter found with type %s for tenant %s",
                                                type,
                                                runtimeTenantResolver.getTenant());
            LOGGER.error(errorMessage);
            throw new OpenSearchUnknownParameter(errorMessage);
        }
        return new HashSet<>(ppties);
    }

    @Override
    public Set<AttributeModel> findAll() {
        return getTenantMap() != null ? Sets.newHashSet(getTenantMap().values()) : Sets.newHashSet();
    }

    @Override
    public String findName(AttributeModel attribute) {
        // Check dynamic properties
        String name = attribute.getFullJsonPath();

        // Only dynamic attributes can have a reduce name path
        if (!attribute.isDynamic() && (attribute.getId() != null)) {
            return name;
        }

        for (Entry<String, AttributeModel> entry : getTenantMap().entrySet()) {
            AttributeModel att = entry.getValue();
            if (att.isDynamic() && (att.getId() != null) && att.getId().equals(attribute.getId())) {
                if (entry.getKey().length() < name.length()) {
                    name = entry.getKey();
                }
            }
        }
        return name;
    }

    @Override
    public void refresh(String tenant) {
        computePropertyMap(tenant);
    }

    private Map<String, AttributeModel> getTenantMap() {
        String tenant = runtimeTenantResolver.getTenant();
        if (!propertyMap.containsKey(tenant)) {
            computePropertyMap(tenant);
        }
        return propertyMap.get(tenant);
    }

    private Multimap<PropertyType, AttributeModel> getTenantTypedMap() {
        String tenant = runtimeTenantResolver.getTenant();
        if (!typedPropertyMap.containsKey(tenant)) {
            computePropertyMap(tenant);
        }
        return typedPropertyMap.get(tenant);
    }

    /**
     * Initialize queryable static properties
     */
    private void initStaticProperties(Map<String, AttributeModel> tenantMap,
                                      Multimap<PropertyType, AttributeModel> tenantTypeMap) {

        // Feature creationDate
        tenantMap.put(StaticProperties.CREATION_DATE,
                      new AttributeModelBuilder(StaticProperties.CREATION_DATE_PATH,
                                                PropertyType.DATE_ISO8601,
                                                null).setStatic(true).setIndexed(true).setInternal(true).build());

        tenantMap.put(StaticProperties.LAST_UPDATE,
                      new AttributeModelBuilder(StaticProperties.LAST_UPDATE_PATH,
                                                PropertyType.DATE_ISO8601,
                                                null).setStatic(true).setIndexed(true).setInternal(true).build());

        // Unique identifier
        tenantMap.put(StaticProperties.FEATURE_ID,
                      new AttributeModelBuilder(StaticProperties.FEATURE_ID, PropertyType.STRING, null).setStatic(true)
                                                                                                       .setIndexed(true)
                                                                                                       .build());

        // Virtual identifier
        tenantMap.put(StaticProperties.FEATURE_VIRTUAL_ID,
                      new AttributeModelBuilder(StaticProperties.FEATURE_VIRTUAL_ID,
                                                PropertyType.STRING,
                                                null).setStatic(true).setIndexed(true).setOptional(true).build());

        // Version
        tenantMap.put(StaticProperties.FEATURE_VERSION,
                      new AttributeModelBuilder(StaticProperties.FEATURE_VERSION, PropertyType.INTEGER, null).setStatic(
                          true).setIndexed(true).build());

        // Is last version
        tenantMap.put(StaticProperties.FEATURE_IS_LAST_VERSION,
                      new AttributeModelBuilder(StaticProperties.FEATURE_IS_LAST_VERSION,
                                                PropertyType.BOOLEAN,
                                                null).setStatic(true).setIndexed(true).build());

        // SIP identifier alias provider identifier
        tenantMap.put(StaticProperties.FEATURE_PROVIDER_ID,
                      new AttributeModelBuilder(StaticProperties.FEATURE_PROVIDER_ID,
                                                PropertyType.STRING,
                                                null).setStatic(true).setIndexed(true).build());

        // Required label for minimal display purpose
        tenantMap.put(StaticProperties.FEATURE_LABEL,
                      new AttributeModelBuilder(StaticProperties.FEATURE_LABEL, PropertyType.STRING, null).setStatic(
                          true).setIndexed(true).build());

        // Related model name
        tenantMap.put(StaticProperties.FEATURE_MODEL,
                      new AttributeModelBuilder(StaticProperties.FEATURE_MODEL, PropertyType.STRING, null).setStatic(
                          true).setIndexed(true).build());

        // // Geometry
        // tenantMap.put(StaticProperties.FEATURE_GEOMETRY, new AttributeModelBuilder
        // (StaticProperties.FEATURE_GEOMETRY, PropertyType.STRING, null).setStatic(true).build());

        // Tags
        tenantMap.put(StaticProperties.FEATURE_TAGS,
                      new AttributeModelBuilder(StaticProperties.FEATURE_TAGS,
                                                PropertyType.STRING,
                                                null).setStatic(true).setIndexed(true).build());

        tenantMap.put(StaticProperties.FEATURE_FILE_RAWDATA_FILENAME,
                      new AttributeModelBuilder(StaticProperties.FEATURE_FILE_RAWDATA_FILENAME_PROPERTY_PATH,
                                                PropertyType.STRING,
                                                null).setStatic(true).setIndexed(true).build());

        // Allows to filter on dataset model id when searching for dataobjects
        tenantMap.put(StaticProperties.DATASET_MODEL_NAMES,
                      new AttributeModelBuilder(StaticProperties.DATASET_MODEL_NAMES,
                                                PropertyType.STRING,
                                                null).setInternal(true).setIndexed(true).build());

        // Register static properties by types
        tenantMap.values().forEach(attModel -> tenantTypeMap.put(attModel.getType(), attModel));
    }

    /**
     * Use the feign client to retrieve all attribute models.<br>
     * The method is private because it is not expected to be used directly, but via its cached facade
     * "getAttributeModels" method.
     */
    protected void computePropertyMap(String tenant) {
        List<AttributeModel> attributeModels = null;
        try {
            runtimeTenantResolver.forceTenant(tenant);
            // Retrieve the list of attribute models
            attributeModels = attributeHelper.getAllAttributes();
        } finally {
            runtimeTenantResolver.clearTenant();
        }
        // Build or rebuild the maps
        Map<String, AttributeModel> tenantMap = new HashMap<>();
        Multimap<PropertyType, AttributeModel> tenantTypeMap = ArrayListMultimap.create();

        // Add static properties
        initStaticProperties(tenantMap, tenantTypeMap);

        // Reference tenant maps (override maybe)
        propertyMap.put(tenant, tenantMap);
        typedPropertyMap.put(tenant, tenantTypeMap);

        // Conflictual dynamic keys to be removed
        List<String> conflictualKeys = new ArrayList<>();

        // Build intelligent map preventing conflicts
        for (AttributeModel attributeModel : attributeModels) {

            // Register properties by types
            tenantTypeMap.put(attributeModel.getType(), attributeModel);

            // - Add mapping between short property name and attribute if no conflict detected
            String key = attributeModel.getName();
            if (!tenantMap.containsKey(key) && !conflictualKeys.contains(key)) {
                // Bind short property name to attribute
                tenantMap.put(key, attributeModel);
            } else {
                // Conflictual dynamic property detected
                if (!conflictualKeys.contains(key)) {
                    // It not yet detected
                    if (tenantMap.get(key).isDynamic()) {
                        conflictualKeys.add(key);
                        tenantMap.remove(key);
                    }
                }
            }

            // - Add mapping between fragment qualified property and attribute
            if (attributeModel.hasFragment()) {
                String fragment = attributeModel.getFragment().getName();
                // Prevent conflicts with static properties
                if (!StaticProperties.FEATURES_STATICS.contains(fragment)) {
                    // Bind fragment qualified property name to attribute
                    tenantMap.put(attributeModel.getJsonPathForNamespace(""), attributeModel);
                }
            }

            // - Add mapping between public json path and attribute model
            tenantMap.put(attributeModel.getJsonPathForNamespace(StaticProperties.FEATURE_PROPERTIES), attributeModel);

            // - Add mapping between fully qualified property and attribute
            tenantMap.put(attributeModel.getFullJsonPath(), attributeModel);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(AttributeModelCreated.class, new CreatedHandler());
        subscriber.subscribeTo(AttributeModelDeleted.class, new DeletedHandler());
        subscriber.subscribeTo(AttributeModelUpdated.class, new UpdateHandler());
        subscriber.subscribeTo(AttributeCacheRefreshEvent.class, new CleanCache());
    }

    protected Map<String, Map<String, AttributeModel>> getPropertyMap() {
        return propertyMap;
    }

    /**
     * Handle {@link AttributeModel} creation
     *
     * @author Xavier-Alexandre Brochard
     */
    private class CreatedHandler implements IHandler<AttributeModelCreated> {

        @Override
        public void handle(TenantWrapper<AttributeModelCreated> pWrapper) {
            try {
                runtimeTenantResolver.forceTenant(pWrapper.getTenant());
                LOGGER.info("Invalidates attributes cache for current tenant as there is a new attribute model {}",
                            pWrapper.getContent().getAttributeName());
                computePropertyMap(pWrapper.getTenant());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    private class CleanCache implements IHandler<AttributeCacheRefreshEvent> {

        @Override
        public void handle(TenantWrapper<AttributeCacheRefreshEvent> pWrapper) {
            try {
                runtimeTenantResolver.forceTenant(pWrapper.getTenant());
                LOGGER.info("Clear attributes cache");
                computePropertyMap(pWrapper.getTenant());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Handle {@link AttributeModel} update
     */
    private class UpdateHandler implements IHandler<AttributeModelUpdated> {

        @Override
        public void handle(TenantWrapper<AttributeModelUpdated> pWrapper) {
            try {
                runtimeTenantResolver.forceTenant(pWrapper.getTenant());
                LOGGER.info("Invalidates attributes cache for current tenant as the attribute model {} is updated",
                            pWrapper.getContent().getAttributeName());
                computePropertyMap(pWrapper.getTenant());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Handle {@link AttributeModel} deletion
     *
     * @author Xavier-Alexandre Brochard
     */
    private class DeletedHandler implements IHandler<AttributeModelDeleted> {

        @Override
        public void handle(TenantWrapper<AttributeModelDeleted> pWrapper) {
            try {
                runtimeTenantResolver.forceTenant(pWrapper.getTenant());
                LOGGER.info("Invalidates attributes cache for current tenant as the attribute model {} was deleted",
                            pWrapper.getContent().getAttributeName());
                computePropertyMap(pWrapper.getTenant());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
