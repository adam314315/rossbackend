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
package fr.cnes.regards.framework.gson.adapters;

import com.google.gson.JsonIOException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;

/**
 * ISO 8601 date adapter
 * This TypeAdapter is used in method GsonAutoConfiguration#customizeBuilder.
 * The aim is to be able to read a date time with or without Time zone specified and to format date time with UTC Time
 * Zone (ie. Z)
 *
 * @author Marc Sordi
 * @author oroussel
 */
public class OffsetDateTimeAdapter extends TypeAdapter<OffsetDateTime> {

    /**
     * ISO date time official support (UTC)
     * When parsing, either no offset, Z or +HH:mm offset.
     * When formatting, Z as offset if UTC or +HH:mm
     */
    public static final DateTimeFormatter ISO_DATE_TIME_UTC = new DateTimeFormatterBuilder().parseCaseInsensitive()
                                                                                            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                                                                            .optionalStart()
                                                                                            .appendOffset("+HH:MM", "Z")
                                                                                            .toFormatter();

    /**
     * Writing date with UTC ISO 8601 format
     */
    @Override
    public void write(JsonWriter out, OffsetDateTime date) throws IOException {
        // truncate to a resolution of 1 microsecond
        out.value(date.atZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS).format(ISO_DATE_TIME_UTC));
    }

    @Override
    public OffsetDateTime read(JsonReader in) throws IOException {
        return parse(in.nextString());
    }

    public static OffsetDateTime parse(String date) {
        try {
            TemporalAccessor temporalAccessor = ISO_DATE_TIME_UTC.parse(date);
            // Zoned date
            if (temporalAccessor.isSupported(ChronoField.OFFSET_SECONDS)) {
                return OffsetDateTime.from(temporalAccessor);
            } else { // No zone specified => UTC date time
                return OffsetDateTime.of(LocalDateTime.from(temporalAccessor), ZoneOffset.UTC);
            }
        } catch (DateTimeParseException e) {
            throw new JsonIOException("Date could not be parsed", e);
        }
    }

    public static String format(OffsetDateTime date) {
        String formattedDate = null;
        if (date != null) {
            // truncate to a resolution of 1 microsecond
            formattedDate = ISO_DATE_TIME_UTC.format(date.withOffsetSameInstant(ZoneOffset.UTC)
                                                         .truncatedTo(ChronoUnit.MICROS));
        }
        return formattedDate;
    }

}
