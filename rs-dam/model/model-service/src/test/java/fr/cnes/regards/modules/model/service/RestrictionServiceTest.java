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
package fr.cnes.regards.modules.model.service;

import fr.cnes.regards.modules.model.domain.attributes.restriction.JsonSchemaRestriction;
import fr.cnes.regards.modules.model.domain.attributes.restriction.RestrictionType;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Restriction service test
 *
 * @author Marc Sordi
 */
public class RestrictionServiceTest {

    /**
     * Real restriction service
     */
    private RestrictionService restrictionService;

    @Before
    public void before() {
        restrictionService = new RestrictionService();
        restrictionService.afterPropertiesSet();
    }

    private void testRestriction(PropertyType propertyType,
                                 int matchedRestrictions,
                                 RestrictionType... acceptableRestrictions) {
        final List<String> restrictions = restrictionService.getRestrictions(propertyType);
        Assert.assertEquals(matchedRestrictions, restrictions.size());
        if (acceptableRestrictions != null) {
            for (RestrictionType acceptable : acceptableRestrictions) {
                Assert.assertTrue(restrictions.contains(acceptable.toString()));
            }
        }
    }

    @Test
    public void getBooleanRestriction() {
        testRestriction(PropertyType.BOOLEAN, 0);
    }

    @Test
    public void getDateRestriction() {
        // Private restriction
        testRestriction(PropertyType.DATE_ARRAY, 0);
        testRestriction(PropertyType.DATE_INTERVAL, 0);
        testRestriction(PropertyType.DATE_ISO8601, 0);
    }

    @Test
    public void getFloatRestriction() {
        testRestriction(PropertyType.DOUBLE, 1, RestrictionType.DOUBLE_RANGE);
        testRestriction(PropertyType.DOUBLE_ARRAY, 1, RestrictionType.DOUBLE_RANGE);
        testRestriction(PropertyType.DOUBLE_INTERVAL, 1, RestrictionType.DOUBLE_RANGE);
    }

    @Test
    public void getIntegerRestriction() {
        testRestriction(PropertyType.INTEGER, 1, RestrictionType.INTEGER_RANGE);
        testRestriction(PropertyType.INTEGER_ARRAY, 1, RestrictionType.INTEGER_RANGE);
        testRestriction(PropertyType.INTEGER_INTERVAL, 1, RestrictionType.INTEGER_RANGE);
    }

    @Test
    public void getStringRestriction() {
        testRestriction(PropertyType.STRING, 2, RestrictionType.PATTERN, RestrictionType.ENUMERATION);
        testRestriction(PropertyType.STRING_ARRAY, 2, RestrictionType.PATTERN, RestrictionType.ENUMERATION);
    }

    @Test
    public void getUrlRestriction() {
        testRestriction(PropertyType.URL, 0);
    }

    @Test
    public void test_valid_json_schema_restriction() {
        JsonSchemaRestriction restriction = new JsonSchemaRestriction();
        restriction.setJsonSchema("{\"type\":\"object\"}");
        Assert.assertTrue(restriction.validate());

        restriction.setJsonSchema("{}");
        Assert.assertTrue(restriction.validate());
    }

    @Test
    public void test_invalid_json_schema_restriction() {
        JsonSchemaRestriction restriction = new JsonSchemaRestriction();
        restriction.setJsonSchema("{\"test\":\"toto\"}");
        Assert.assertFalse(restriction.validate());
    }

}
