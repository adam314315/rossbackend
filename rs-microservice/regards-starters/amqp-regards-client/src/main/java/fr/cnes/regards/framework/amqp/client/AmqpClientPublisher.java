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
package fr.cnes.regards.framework.amqp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.IEvent;
import fr.cnes.regards.framework.random.Generator;
import fr.cnes.regards.framework.random.GeneratorBuilder;
import fr.cnes.regards.framework.random.function.IPropertyGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@SuppressWarnings("unchecked")
public class AmqpClientPublisher {

    private final static String TEMPLATE_REGEXP = "^.*-template.json";

    private final static Pattern TEMPLATE_PATTERN = Pattern.compile(TEMPLATE_REGEXP);

    private final static String HEADERS_NS = "$headers";

    private final static Integer BATCH_SIZE = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpClientApplication.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IPropertyGetter propertyGetter;

    @Autowired
    private GeneratorBuilder generatorBuilder;

    /**
     * Publish a single message loaded from specified JSON file
     */
    public void publish(String exchangeName,
                        Optional<String> queueName,
                        Optional<String> routingKey,
                        Optional<String> dlk,
                        Integer priority,
                        Map<String, Object> headers,
                        String jsonPathString,
                        Integer iterations) {

        Path path = Paths.get(jsonPathString);
        if (!Files.exists(path)) {
            String error = String.format("Unknown json path %s", path);
            LOGGER.error(error);
            throw new IllegalArgumentException(error);
        }

        if (Files.isDirectory(path)) {
            doPublishAll(exchangeName, queueName, routingKey, dlk, priority, headers, path, iterations);
        } else {
            Matcher matcher = TEMPLATE_PATTERN.matcher(jsonPathString);
            // Check if it is a template
            if (matcher.matches()) {
                LOGGER.info("Handling JSON template");
                doPublishWithTemplate(exchangeName, queueName, routingKey, dlk, priority, headers, path, iterations);
            } else {
                doPublish(exchangeName, queueName, routingKey, dlk, priority, headers, path);
            }
        }
    }

    /**
     * Publish all messages generated from specified template.
     */
    private void doPublishWithTemplate(String exchangeName,
                                       Optional<String> queueName,
                                       Optional<String> routingKey,
                                       Optional<String> dlk,
                                       Integer priority,
                                       Map<String, Object> headers,
                                       Path templatePath,
                                       Integer iterations) {
        // Generate messages
        Generator randomGenerator = generatorBuilder.build(templatePath, propertyGetter);

        Integer remaining = iterations;
        while (remaining > 0) {
            Integer batchSize = remaining >= BATCH_SIZE ? BATCH_SIZE : remaining;
            remaining = remaining - batchSize;
            // Generate batch
            List<Map<String, Object>> messages = randomGenerator.generate(batchSize);
            List<IEvent> oMessages = new ArrayList<>();
            messages.forEach(m -> oMessages.add(manageHeaders(m)));
            // Broadcast
            publisher.broadcastAll(exchangeName, queueName, routingKey, dlk, priority, oMessages, headers);
            LOGGER.info("Batch of {} messages sended. Remaining {}.", batchSize, remaining);
        }
    }

    /**
     * Publish all messages load from specified directory.
     */
    private void doPublishAll(String exchangeName,
                              Optional<String> queueName,
                              Optional<String> routingKey,
                              Optional<String> dlk,
                              Integer priority,
                              Map<String, Object> headers,
                              Path jsonPath,
                              Integer iterations) {

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.json");

        try (Stream<Path> walk = Files.walk(jsonPath)) {
            walk.filter(Files::isRegularFile).filter(p -> matcher.matches(p)).forEach(p -> {
                Matcher patternmatcher = TEMPLATE_PATTERN.matcher(p.toString());
                // Check if it is a template
                if (patternmatcher.matches()) {
                    LOGGER.info("Handling JSON template");
                    doPublishWithTemplate(exchangeName, queueName, routingKey, dlk, priority, headers, p, iterations);
                } else {
                    doPublish(exchangeName, queueName, routingKey, dlk, priority, headers, p);
                }
            });

        } catch (IOException e) {
            String error = String.format("Error inspecting directory : %s", jsonPath);
            LOGGER.error(error, e);
            throw new IllegalArgumentException(error);
        }

    }

    private void doPublish(String exchangeName,
                           Optional<String> queueName,
                           Optional<String> routingKey,
                           Optional<String> dlk,
                           Integer priority,
                           Map<String, Object> headers,
                           Path jsonPath) {
        try {
            LOGGER.info("Loading JSON from {}", jsonPath);
            // Load JSON message
            Map<String, Object> readMessage = mapper.readValue(jsonPath.toFile(), Map.class);
            IEvent message = manageHeaders(readMessage);
            // Broadcast
            publisher.broadcast(exchangeName, queueName, routingKey, dlk, priority, message, headers);
        } catch (IOException e) {
            String error = String.format("Cannot read json from path %s", jsonPath);
            LOGGER.error(error, e);
            throw new IllegalArgumentException(error);
        }
    }

    private IEvent manageHeaders(Map<String, Object> message) {
        MessageWithHeaders<String, Object> mwh = new MessageWithHeaders<String, Object>();
        Object oHeaders = message.remove(HEADERS_NS);
        mwh.putAll(message);
        if ((oHeaders != null) && Map.class.isAssignableFrom(oHeaders.getClass())) {
            // Else prepare a message with propagated headers
            Map<String, Object> headers = (Map<String, Object>) oHeaders;
            headers.forEach((k, v) -> mwh.setHeader(k, v));
        }
        return mwh;
    }
}
