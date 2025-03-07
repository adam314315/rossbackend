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
package fr.cnes.regards.modules.opensearch.service.parser;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.PolygonCriterion;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Unit test for {@link GeometryParser}
 *
 * @author Xavier-Alexandre Brochard
 */
public class GeometryParserTest {

    /**
     * The tested parser
     */
    private static final GeometryParser PARSER = new GeometryParser();

    @Test
    @Requirement("REGARDS_DSL_DAM_PLG_250")
    @Purpose("Test queries like g=POLYGON((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2))")
    public final void testParse_shouldParseGeometry() throws OpenSearchParseException, UnsupportedEncodingException {
        String request = URLEncoder.encode(
            "POLYGON((0 0, -0.5 0.5, 0 1,  0.5 1,1 0.5, 0.5 0,0 0), (0.5 0.2, 0.6 0.5, 0.2 0.9,-0.2 0.5, 0.1 0.2, 0.2"
            + " 0.3, 0.5 0.2))",
            StandardCharsets.UTF_8);
        ICriterion criterion = PARSER.parse("g=" + request);
        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof PolygonCriterion);

        final PolygonCriterion crit = (PolygonCriterion) criterion;
        // External ring
        MatcherAssert.assertThat(crit.getCoordinates()[0][0][0], Matchers.equalTo(0.0));
        MatcherAssert.assertThat(crit.getCoordinates()[0][0][1], Matchers.equalTo(0.0));

        MatcherAssert.assertThat(crit.getCoordinates()[0][1][0], Matchers.equalTo(-0.5));
        MatcherAssert.assertThat(crit.getCoordinates()[0][1][1], Matchers.equalTo(0.5));

        MatcherAssert.assertThat(crit.getCoordinates()[0][2][0], Matchers.equalTo(0.0));
        MatcherAssert.assertThat(crit.getCoordinates()[0][2][1], Matchers.equalTo(1.0));

        MatcherAssert.assertThat(crit.getCoordinates()[0][3][0], Matchers.equalTo(0.5));
        MatcherAssert.assertThat(crit.getCoordinates()[0][3][1], Matchers.equalTo(1.0));

        MatcherAssert.assertThat(crit.getCoordinates()[0][4][0], Matchers.equalTo(1.0));
        MatcherAssert.assertThat(crit.getCoordinates()[0][4][1], Matchers.equalTo(0.5));

        MatcherAssert.assertThat(crit.getCoordinates()[0][5][0], Matchers.equalTo(0.5));
        MatcherAssert.assertThat(crit.getCoordinates()[0][5][1], Matchers.equalTo(0.0));

        MatcherAssert.assertThat(crit.getCoordinates()[0][6][0], Matchers.equalTo(0.0));
        MatcherAssert.assertThat(crit.getCoordinates()[0][6][1], Matchers.equalTo(0.0));

        // Internal ring
        MatcherAssert.assertThat(crit.getCoordinates()[1][0][0], Matchers.equalTo(0.5));
        MatcherAssert.assertThat(crit.getCoordinates()[1][0][1], Matchers.equalTo(0.2));

        MatcherAssert.assertThat(crit.getCoordinates()[1][1][0], Matchers.equalTo(0.6));
        MatcherAssert.assertThat(crit.getCoordinates()[1][1][1], Matchers.equalTo(0.5));

        MatcherAssert.assertThat(crit.getCoordinates()[1][2][0], Matchers.equalTo(0.2));
        MatcherAssert.assertThat(crit.getCoordinates()[1][2][1], Matchers.equalTo(0.9));

        MatcherAssert.assertThat(crit.getCoordinates()[1][3][0], Matchers.equalTo(-0.2));
        MatcherAssert.assertThat(crit.getCoordinates()[1][3][1], Matchers.equalTo(0.5));

        MatcherAssert.assertThat(crit.getCoordinates()[1][4][0], Matchers.equalTo(0.1));
        MatcherAssert.assertThat(crit.getCoordinates()[1][4][1], Matchers.equalTo(0.2));

        MatcherAssert.assertThat(crit.getCoordinates()[1][5][0], Matchers.equalTo(0.2));
        MatcherAssert.assertThat(crit.getCoordinates()[1][5][1], Matchers.equalTo(0.3));

        MatcherAssert.assertThat(crit.getCoordinates()[1][6][0], Matchers.equalTo(0.5));
        MatcherAssert.assertThat(crit.getCoordinates()[1][6][1], Matchers.equalTo(0.2));
    }

    @Test(expected = OpenSearchParseException.class)
    @Requirement("REGARDS_DSL_DAM_PLG_250")
    @Purpose("Test queries like g=MULTILINESTRING((3 4,10 50,20 25),(-5 -8,-10 -8,-15 -4))")
    public final void testParse_shouldFailIfNotPolygon() throws OpenSearchParseException {
        String request = URLEncoder.encode("MULTILINESTRING((3 4,10 50,20 25),(-5 -8,-10 -8,-15 -4))",
                                           StandardCharsets.UTF_8);
        PARSER.parse("g=" + request);
    }

    @Test(expected = OpenSearchParseException.class)
    @Requirement("REGARDS_DSL_DAM_PLG_250")
    @Purpose("Test invalid polygon with same successive points like g=POLYGON(0 0,0 0,0 0, 0 0, 0 0)")
    public final void testParse_shouldFailIfInvalidPolygon() throws OpenSearchParseException {
        String request = URLEncoder.encode("POLYGON((0 0,0 0,0 0, 0 0, 0 0))", StandardCharsets.UTF_8);
        PARSER.parse("g=" + request);
    }

}
