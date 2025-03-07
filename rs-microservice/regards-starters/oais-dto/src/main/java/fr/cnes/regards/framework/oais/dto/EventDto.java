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
package fr.cnes.regards.framework.oais.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * OAIS event representation
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 */
public class EventDto {

    /**
     * Custom event type
     */
    @Schema(description = "Event type.", example = "Creation")
    private String type;

    @NotBlank
    @Schema(description = "Event description.", example = "package creation")
    private String comment;

    @NotNull
    @Schema(description = "Event submission date.", example = "2024-12-23 05:00:00")
    private OffsetDateTime date;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EventDto event = (EventDto) o;

        if (type != null ? !type.equals(event.type) : event.type != null) {
            return false;
        }
        if (!comment.equals(event.comment)) {
            return false;
        }
        return date.isEqual(event.date);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + comment.hashCode();
        result = 31 * result + date.hashCode();
        return result;
    }
}
