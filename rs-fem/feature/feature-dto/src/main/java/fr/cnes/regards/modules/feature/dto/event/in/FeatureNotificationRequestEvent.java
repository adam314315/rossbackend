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
package fr.cnes.regards.modules.feature.dto.event.in;

import fr.cnes.regards.framework.amqp.event.*;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.dto.urn.converter.FeatureUrnConverter;
import jakarta.persistence.Convert;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Request for feature notification using event driven mechanism
 *
 * @author Kevin Marchois
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class FeatureNotificationRequestEvent extends AbstractRequestEvent implements ISubscribable {

    @NotNull
    @Convert(converter = FeatureUrnConverter.class)
    private FeatureUniformResourceName urn;

    @NotNull(message = "Priority level is required")
    private PriorityLevel priority;

    /**
     * List of recipients(business identifiers) for the direct notification
     */
    private Set<String> recipients;

    public FeatureUniformResourceName getUrn() {
        return urn;
    }

    public void setUrn(FeatureUniformResourceName urn) {
        this.urn = urn;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

    public Set<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(Set<String> recipients) {
        this.recipients = recipients;
    }

    public static FeatureNotificationRequestEvent build(String requestOwner,
                                                        FeatureUniformResourceName urn,
                                                        PriorityLevel priority,
                                                        Set<String> recipients) {
        return build(requestOwner, urn, OffsetDateTime.now().minusSeconds(1), priority, recipients);
    }

    public static FeatureNotificationRequestEvent build(String requestOwner,
                                                        FeatureUniformResourceName urn,
                                                        OffsetDateTime requestDate,
                                                        PriorityLevel priority,
                                                        Set<String> recipients) {
        FeatureNotificationRequestEvent event = new FeatureNotificationRequestEvent();
        event.setRequestId(generateRequestId());
        event.setRequestOwner(requestOwner);
        event.setRequestDate(requestDate);
        event.setPriority(priority);
        event.setUrn(urn);
        event.setRecipients(recipients);

        return event;
    }

}
