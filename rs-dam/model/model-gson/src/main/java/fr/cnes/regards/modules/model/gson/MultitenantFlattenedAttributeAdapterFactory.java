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
package fr.cnes.regards.modules.model.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fr.cnes.regards.framework.gson.adapters.MultitenantPolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactoryBean;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.dto.properties.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Manage dynamic attribute (de)serialization
 *
 * @author Marc Sordi
 */
@SuppressWarnings("rawtypes")
@GsonTypeAdapterFactoryBean
public class MultitenantFlattenedAttributeAdapterFactory extends MultitenantPolymorphicTypeAdapterFactory<IProperty> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultitenantFlattenedAttributeAdapterFactory.class);

    /**
     * Discriminator field
     */
    private static final String DISCRIMINATOR_FIELD_NAME = "name";

    /**
     * Discriminator field
     */
    private static final String VALUE_FIELD_NAME = "value";

    /**
     * Namespace separator
     */
    private static final String NS_SEPARATOR = ".";

    /**
     * Regexp escape character
     */
    private static final String REGEXP_ESCAPE = "\\";

    public MultitenantFlattenedAttributeAdapterFactory(final IRuntimeTenantResolver pRuntimeTenantResolver) {
        super(pRuntimeTenantResolver, IProperty.class, DISCRIMINATOR_FIELD_NAME);
        runtimeTenantResolver = pRuntimeTenantResolver;
    }

    public void registerSubtype(final String pTenant,
                                final Class<?> pType,
                                final String pDiscriminatorFieldValue,
                                final String pNamespace) {
        if (pNamespace == null) {
            registerSubtype(pTenant, pType, pDiscriminatorFieldValue);
        } else {
            registerSubtype(pTenant, pType, pNamespace.concat(NS_SEPARATOR).concat(pDiscriminatorFieldValue));
        }
    }

    public void unregisterSubtype(final String pTenant,
                                  final Class<?> pType,
                                  final String pDiscriminatorFieldValue,
                                  final String pNamespace) {
        if (pNamespace == null) {
            unregisterSubtype(pTenant, pType, pDiscriminatorFieldValue);
        } else {
            unregisterSubtype(pTenant, pType, pNamespace.concat(NS_SEPARATOR).concat(pDiscriminatorFieldValue));
        }
    }

    /**
     * Dynamically register configured {@link AttributeModel} for a particular tenant
     *
     * @param pTenant tenant
     */
    public void registerAttributes(final String pTenant, final List<AttributeModel> pAttributes) {
        if (pAttributes != null) {
            for (AttributeModel att : pAttributes) {
                LOGGER.debug("{} - Registering attribute {} - {} - {}",
                             pTenant,
                             att.getName(),
                             att.getFullName(),
                             att.getFullJsonPath());
                registerAttribute(pTenant, att);
            }
        }
    }

    /**
     * Register attribute
     *
     * @param pTenant tenant
     * @param att     the attribute containing its fragment with its name and its type and name.
     */
    public void registerAttribute(String pTenant, AttributeModel att) {
        if (!att.isVirtual()) {
            // Define namespace if required
            String namespace = null;
            // Register namespace as an object wrapper
            if (!att.getFragment().isDefaultFragment()) {
                namespace = att.getFragment().getName();
                registerSubtype(pTenant, ObjectProperty.class, namespace);
            }

            // Register attribute
            registerSubtype(pTenant, getClassByType(att.getType()), att.getName(), namespace);
        }
    }

    /**
     * Unregister attribute
     *
     * @param pTenant tenant
     * @param att     the attribute containing its fragment with its name and its type and name.
     */
    public void unregisterAttribute(String pTenant, AttributeModel att) {
        // Define namespace if required
        String namespace = null;
        // Register namespace as an object wrapper
        if (!att.getFragment().isDefaultFragment()) {
            namespace = att.getFragment().getName();
        }

        // Unregister attribute
        unregisterSubtype(pTenant, getClassByType(att.getType()), att.getName(), namespace);
    }

    /**
     * Unregister fragment mapping
     *
     * @param pTenant  tenant
     * @param fragment the fragment containing its name
     */
    public void unregisterFragment(String pTenant, Fragment fragment) {
        if (!fragment.isDefaultFragment()) {
            // Unregister fragment
            unregisterSubtype(pTenant, ObjectProperty.class, fragment.getName());
        }
    }

    public void refresh(final String pTenant, final List<AttributeModel> pAttributes) {
        registerAttributes(pTenant, pAttributes);
    }

    /**
     * @param pAttributeType {@link PropertyType}
     * @return corresponding {@link Serializable} class
     */
    public Class<?> getClassByType(PropertyType pAttributeType) { // NOSONAR
        // Retrieve matching attribute class
        Class<?> matchingClass;
        switch (pAttributeType) {
            case BOOLEAN:
                matchingClass = BooleanProperty.class;
                break;
            case DATE_ARRAY:
                matchingClass = DateArrayProperty.class;
                break;
            case DATE_RANGE:
                matchingClass = DateRangeProperty.class;
                break;
            case DATE_INTERVAL:
                matchingClass = DateIntervalProperty.class;
                break;
            case DATE_ISO8601:
                matchingClass = DateProperty.class;
                break;
            case DOUBLE:
                matchingClass = DoubleProperty.class;
                break;
            case DOUBLE_ARRAY:
                matchingClass = DoubleArrayProperty.class;
                break;
            case DOUBLE_RANGE:
                matchingClass = DoubleRangeProperty.class;
                break;
            case DOUBLE_INTERVAL:
                matchingClass = DoubleIntervalProperty.class;
                break;
            case INTEGER:
                matchingClass = IntegerProperty.class;
                break;
            case INTEGER_ARRAY:
                matchingClass = IntegerArrayProperty.class;
                break;
            case INTEGER_RANGE:
                matchingClass = IntegerRangeProperty.class;
                break;
            case INTEGER_INTERVAL:
                matchingClass = IntegerIntervalProperty.class;
                break;
            case STRING:
                matchingClass = StringProperty.class;
                break;
            case STRING_ARRAY:
                matchingClass = StringArrayProperty.class;
                break;
            case URL:
                matchingClass = UrlProperty.class;
                break;
            case LONG:
                matchingClass = LongProperty.class;
                break;
            case LONG_ARRAY:
                matchingClass = LongArrayProperty.class;
                break;
            case LONG_RANGE:
                matchingClass = LongRangeProperty.class;
                break;
            case LONG_INTERVAL:
                matchingClass = LongIntervalProperty.class;
                break;
            case JSON:
                matchingClass = JsonProperty.class;
                break;
            default:
                final String errorMessage = String.format("Unexpected attribute type \"%s\".", pAttributeType);
                LOGGER.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
        }
        return matchingClass;
    }

    @Override
    protected JsonElement getOnReadDiscriminator(final JsonElement pJsonElement) {
        final JsonElement discriminator = null;
        if (pJsonElement.isJsonObject()) {
            final JsonObject o = pJsonElement.getAsJsonObject();
            if (o.size() != 1) {
                final String errorMessage = String.format("Only single key/value pair is expected in \"%s\"",
                                                          pJsonElement);
                LOGGER.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
            for (final Map.Entry<String, JsonElement> entry : o.entrySet()) {
                return new JsonPrimitive(entry.getKey());
            }
        }
        return discriminator;
    }

    @Override
    protected JsonElement beforeRead(final JsonElement pJsonElement,
                                     final String pDiscriminator,
                                     final Class<?> pSubType) {
        final JsonElement restored = restore(pJsonElement, pSubType);
        if (pSubType == ObjectProperty.class) {
            addNamespaceToChildren(restored, pDiscriminator);
        }
        removeParentNamespace(restored);
        return restored;
    }

    @Override
    protected JsonElement beforeWrite(final JsonElement pJsonElement, final Class<?> pSubType) {
        return flatten(pJsonElement, pSubType);
    }

    /**
     * Flatten a {@link JsonElement} carrying key and value in separated fields into a single field whose key is the
     * value of the key field and value the value of the value field
     *
     * @param pJsonElement {@link JsonElement} to flatten
     * @param pSubType     sub type
     * @return flattened {@link JsonElement}
     */
    protected JsonElement flatten(final JsonElement pJsonElement, final Class<?> pSubType) {
        LOGGER.debug(String.format("Flattening %s", pJsonElement));

        if (!pJsonElement.isJsonObject()) {
            final String format = "JSON element must be an object containing 2 members whose names are \"%s\" and \"%s\"";
            final String errorMessage = String.format(format, DISCRIMINATOR_FIELD_NAME, VALUE_FIELD_NAME);
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        final JsonObject current = pJsonElement.getAsJsonObject();

        // Init flattened element
        final JsonObject flattened = new JsonObject();
        // Get key : must be a string
        final JsonElement key = current.get(DISCRIMINATOR_FIELD_NAME);
        // Get value
        final JsonElement val = current.get(VALUE_FIELD_NAME);

        if (pSubType == ObjectProperty.class) {
            // Flattening array elements
            final JsonObject flattenedObject = new JsonObject();
            final Iterator<JsonElement> nestedIter = val.getAsJsonArray().iterator();
            while (nestedIter.hasNext()) {
                final JsonObject nested = nestedIter.next().getAsJsonObject();
                for (final Map.Entry<String, JsonElement> e : nested.entrySet()) {
                    flattenedObject.add(e.getKey(), e.getValue());
                }
            }
            flattened.add(key.getAsString(), flattenedObject);
        } else {
            flattened.add(key.getAsString(), val);
        }

        LOGGER.debug(String.format("Flattened object : \"%s\" -> \"%s\"", pJsonElement, flattened));

        return flattened;
    }

    /**
     * Restore {@link JsonElement} object structure (inverse flattening)
     *
     * @param pJsonElement {@link JsonElement} to restore
     * @param pSubType     sub type
     * @return restored {@link JsonElement}
     */
    protected JsonElement restore(final JsonElement pJsonElement, final Class<?> pSubType) {
        LOGGER.debug(String.format("Restoring %s", pJsonElement));

        if (!pJsonElement.isJsonObject()) {
            final String errorMessage = "JSON element must be an object.";
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        final JsonObject current = pJsonElement.getAsJsonObject();

        // Init restored element
        final JsonObject restored = new JsonObject();
        // Restore members
        for (final Map.Entry<String, JsonElement> e : current.entrySet()) {
            restored.addProperty(DISCRIMINATOR_FIELD_NAME, e.getKey());
            final JsonElement val = e.getValue();
            if (pSubType == ObjectProperty.class) {
                // Restoring array but not element structure
                final JsonArray restoredArray = new JsonArray();
                for (final Map.Entry<String, JsonElement> nestedEntry : val.getAsJsonObject().entrySet()) {
                    final JsonObject nestedObject = new JsonObject();
                    nestedObject.add(nestedEntry.getKey(), nestedEntry.getValue());
                    restoredArray.add(nestedObject);
                }
                restored.add(VALUE_FIELD_NAME, restoredArray);
            } else {
                restored.add(VALUE_FIELD_NAME, val);
            }
        }

        LOGGER.debug(String.format("Restored object : \"%s\" -> \"%s\"", pJsonElement, restored));

        return restored;
    }

    /**
     * Add namespace to {@link JsonElement} children of {@link ObjectProperty}
     *
     * @param pJsonElement   {@link JsonElement}
     * @param pDiscriminator discriminator value
     */
    protected void addNamespaceToChildren(final JsonElement pJsonElement, final String pDiscriminator) {

        if (pJsonElement.isJsonObject()) {
            final JsonElement children = pJsonElement.getAsJsonObject().get(VALUE_FIELD_NAME);

            if (children == null) {
                throw missingFieldException(pJsonElement, VALUE_FIELD_NAME);
            }

            if (children.isJsonArray()) {
                final Iterator<JsonElement> childrenIter = children.getAsJsonArray().iterator();
                while (childrenIter.hasNext()) {
                    addNamespaceToChild(childrenIter.next(), pDiscriminator);
                }
            } else {
                throw objectRequiredException(pJsonElement);
            }
        } else {
            throw objectRequiredException(pJsonElement);
        }
    }

    /**
     * Add namespace to {@link JsonElement} child keys
     *
     * @param pJsonElement   {@link JsonElement}
     * @param pDiscriminator discriminator value
     */
    protected void addNamespaceToChild(final JsonElement pJsonElement, final String pDiscriminator) {

        if (pJsonElement.isJsonObject()) {

            // Backup for logging
            final String logOriginal = pJsonElement.toString();
            final JsonObject o = pJsonElement.getAsJsonObject();

            for (final Map.Entry<String, JsonElement> entry : o.entrySet()) {
                // Add new key mapping
                o.add(pDiscriminator.concat(NS_SEPARATOR).concat(entry.getKey()), entry.getValue());
                // Remove old key mapping
                o.remove(entry.getKey());
            }

            LOGGER.debug(String.format("Namespace added : \"%s\" -> \"%s\"", logOriginal, pJsonElement.toString()));
        } else {
            throw objectRequiredException(pJsonElement);
        }
    }

    /**
     * Remove namespace from {@link JsonElement}
     *
     * @param jsonElement target {@link JsonElement}
     */
    protected void removeParentNamespace(final JsonElement jsonElement) {
        if (jsonElement.isJsonObject()) {

            // Backup for logging
            final String logOriginal = jsonElement.toString();

            final JsonObject o = jsonElement.getAsJsonObject();
            final JsonElement nsElement = o.get(DISCRIMINATOR_FIELD_NAME);
            if (nsElement == null) {
                throw missingFieldException(jsonElement, DISCRIMINATOR_FIELD_NAME);
            }

            // Compute and inject name without its namespace
            final String nsName = nsElement.getAsString();
            final String[] splitNsName = nsName.split(REGEXP_ESCAPE + NS_SEPARATOR);
            o.add(DISCRIMINATOR_FIELD_NAME, new JsonPrimitive(splitNsName[splitNsName.length - 1]));

            if (splitNsName.length > 1) {
                LOGGER.debug(String.format("Namespace removed : \"%s\" -> \"%s\"",
                                           logOriginal,
                                           jsonElement.toString()));
            } else {
                LOGGER.debug(String.format("No namespace to remove : \"%s\" -> \"%s\"",
                                           logOriginal,
                                           jsonElement.toString()));
            }
        } else {
            throw objectRequiredException(jsonElement);
        }
    }

    private IllegalArgumentException objectRequiredException(final JsonElement pJsonElement) {
        final String errorMessage = String.format("Unexpected JSON element %s. Object required.",
                                                  pJsonElement.toString());
        LOGGER.error(errorMessage);
        return new IllegalArgumentException(errorMessage);
    }

    private IllegalArgumentException missingFieldException(final JsonElement pJsonElement, final String pFieldName) {
        final String errorMessage = String.format("JSON element %s must contains a \"%s\" field",
                                                  pJsonElement.toString(),
                                                  pFieldName);
        LOGGER.error(errorMessage);
        return new IllegalArgumentException(errorMessage);
    }
}
