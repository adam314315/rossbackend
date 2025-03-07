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
package fr.cnes.regards.framework.utils.plugins;

import com.google.gson.*;
import fr.cnes.regards.framework.gson.GsonCustomizer;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Gson service for plugin parameter transformation
 *
 * @author Marc SORDI
 */
public class PluginParameterTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginParameterTransformer.class);

    private static final String TRANSFO_MESSAGE = "Transforming value for parameter {} of type {}";

    private static final String SKIP_TRANSFO_MESSAGE = "Skip tranformation for parameter {}";

    private static Gson gsonInstance;

    private PluginParameterTransformer() {
    }

    public static void setup(Gson gson) {
        gsonInstance = gson;
        if (gsonInstance == null) {
            LOGGER.info("Configuring development GSON instance");
            GsonBuilder builder = GsonCustomizer.gsonBuilder(Optional.empty(), Optional.empty());
            gsonInstance = builder.create();
        }
    }

    /**
     * Get parameter value for complex parameter type.
     * Process value transformation on {@link JsonElement} to expected plugin pararameter JAVA type.
     *
     * @param param plugin parameter
     * @param field field to set
     * @return value as a class instance
     */
    protected static Object getParameterValue(IPluginParam param, Field field) {
        Object value = null;

        // Check type transformation constistency
        if (PluginParamType.COLLECTION.equals(param.getType())) {
            value = getCollectionValue(param, field);
        } else if (PluginParamType.MAP.equals(param.getType())) {
            value = getMapValue(param, field);
        } else if (PluginParamType.POJO.equals(param.getType())) {
            value = getPojoValue(param, field);
        } else {
            String message = String.format("Value transformation not available for parameter \"%s\" of type \"%s\"",
                                           param.getName(),
                                           param.getType());
            LOGGER.error(message);
            throw new PluginUtilsRuntimeException(message);
        }

        return value;
    }

    private static Object getCollectionValue(IPluginParam param, Field field) {
        if (JsonCollectionPluginParam.class.isAssignableFrom(param.getClass())) {
            LOGGER.debug(TRANSFO_MESSAGE, param.getName(), param.getType());
            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
            // Get first generic type
            return transformValue((JsonCollectionPluginParam) param,
                                  field.getType(),
                                  parameterizedType.getActualTypeArguments()[0]);
        } else {
            LOGGER.debug(SKIP_TRANSFO_MESSAGE, param.getName());
            return param.getValue();
        }
    }

    private static Object getMapValue(IPluginParam param, Field field) {
        if (JsonMapPluginParam.class.isAssignableFrom(param.getClass())) {
            LOGGER.debug(TRANSFO_MESSAGE, param.getName(), param.getType());
            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
            // Get second generic type
            return transformValue((JsonMapPluginParam) param, parameterizedType.getActualTypeArguments()[1]);
        } else {
            LOGGER.debug(SKIP_TRANSFO_MESSAGE, param.getName());
            return param.getValue();
        }
    }

    private static Object getPojoValue(IPluginParam param, Field field) {
        if (JsonObjectPluginParam.class.isAssignableFrom(param.getClass())) {
            LOGGER.debug(TRANSFO_MESSAGE, param.getName(), param.getType());
            return transformValue((JsonObjectPluginParam) param, field.getType());
        } else {
            LOGGER.debug(SKIP_TRANSFO_MESSAGE, param.getName());
            return param.getValue();
        }
    }

    public static Object transformValue(JsonObjectPluginParam source, Type type) {
        try {
            return source.getValue() == null ? null : gsonInstance.fromJson(source.getValue(), type);
        } catch (JsonSyntaxException e) {
            throw propagateException(e, source);
        }
    }

    public static JsonObject toJson(Object value) {
        JsonElement el = gsonInstance.toJsonTree(value);
        if (!el.isJsonObject()) {
            throw new IllegalArgumentException("POJO is required!");
        }
        return (JsonObject) el;
    }

    public static Collection<Object> transformValue(JsonCollectionPluginParam source, Class<?> rawType, Type type) {
        try {
            Collection<Object> collection = null;
            if (source.getValue() != null && !source.getValue().isEmpty()) {
                if (rawType.equals(Set.class)) {
                    collection = new HashSet<>();
                } else if (rawType.equals(List.class)) {
                    collection = new ArrayList<>();
                } else {
                    throw new IllegalArgumentException(String.format("Unsupported collection type %s", rawType));
                }

                for (JsonElement el : source.getValue()) {
                    Object o = gsonInstance.fromJson(el, type);
                    collection.add(o);
                }
            }
            return collection;
        } catch (JsonSyntaxException e) {
            throw propagateException(e, source);
        }
    }

    public static Collection<JsonElement> toJson(Collection<?> value) {
        Collection<JsonElement> collection = new ArrayList<>();
        for (Object o : value) {
            collection.add(gsonInstance.toJsonTree(o));
        }
        return collection;
    }

    public static Map<String, Object> transformValue(JsonMapPluginParam source, Type type) {
        try {
            Map<String, Object> map = null;
            if (source.getValue() != null && !source.getValue().isEmpty()) {
                map = new HashMap<>();
                for (Entry<String, JsonElement> entry : source.getValue().entrySet()) {
                    Object o = gsonInstance.fromJson(entry.getValue(), type);
                    map.put(entry.getKey(), o);
                }
            }
            return map;
        } catch (JsonSyntaxException e) {
            throw propagateException(e, source);
        }
    }

    public static Map<String, JsonElement> toJson(Map<String, ?> value) {
        ConcurrentMap<String, JsonElement> map = new ConcurrentHashMap<>();
        for (Entry<String, ?> entry : value.entrySet()) {
            map.put(entry.getKey(), gsonInstance.toJsonTree(entry.getValue()));
        }
        return map;
    }

    private static PluginUtilsRuntimeException propagateException(Throwable t, IPluginParam source) {
        String message = String.format("Cannot transform \"%s\" parameter value with name \"%s\"",
                                       source.getType(),
                                       source.getName());
        LOGGER.error(message, t);
        return new PluginUtilsRuntimeException(message, t);
    }
}
