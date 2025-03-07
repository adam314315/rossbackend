/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.entity.converter;

import com.google.gson.Gson;
import io.vavr.collection.List;

/**
 * This class defines custom converters for Gson.
 *
 * @author gandrieu
 */
public class DaoCustomConverters {

    private DaoCustomConverters() {
    }

    public static java.util.List<Object> getCustomConverters(Gson gson) {
        return List.<Object>of(new ParamValuesToJsonbConverter(gson),
                               new JsonbToParamValuesConverter(gson),
                               new FileStatsByDatasetToJsonbConverter(gson),
                               new JsonbToFileStatsByDatasetConverter(gson),
                               new FileParametersToJsonbConverter(gson),
                               new JsonbToFileParametersConverter(gson),
                               new StepsToJsonbConverter(gson),
                               new JsonbToStepsConverter(gson)).toJavaList();
    }

}
