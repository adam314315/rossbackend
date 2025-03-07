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
package fr.cnes.regards.modules.model.dto.properties.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapter;
import fr.cnes.regards.modules.model.dto.properties.MarkdownURL;

import java.io.IOException;

/**
 * @author sbinda
 */
@GsonTypeAdapter(adapted = MarkdownURL.class)
public class MarkdownURLAdapter extends TypeAdapter<MarkdownURL> {

    @Override
    public void write(JsonWriter out, MarkdownURL value) throws IOException {
        if (value != null) {
            out.value(value.toString());
        } else {
            out.nullValue();
        }
    }

    @Override
    public MarkdownURL read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            // read the null anyway so that reader is not stuck
            in.nextNull();
            return MarkdownURL.build(null);
        } else {
            return MarkdownURL.build(in.nextString());
        }
    }
}
