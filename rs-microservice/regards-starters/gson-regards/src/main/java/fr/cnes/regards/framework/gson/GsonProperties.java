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
package fr.cnes.regards.framework.gson;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GSON properties
 *
 * @author Marc Sordi
 */
@ConfigurationProperties(prefix = "regards.gson")
public class GsonProperties {

    private String scanPrefix = "fr.cnes.regards";

    private Boolean prettyPrint = Boolean.FALSE;

    private Boolean serializeNulls = Boolean.FALSE;

    public String getScanPrefix() {
        return scanPrefix;
    }

    public void setScanPrefix(String pScanPrefix) {
        scanPrefix = pScanPrefix;
    }

    public Boolean getPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(Boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public Boolean getSerializeNulls() {
        return serializeNulls;
    }

    public void setSerializeNulls(Boolean serializeNulls) {
        this.serializeNulls = serializeNulls;
    }

}
