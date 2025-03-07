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
package fr.cnes.regards.framework.oais.dto.urn;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.EntityType;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * @author Sylvain Vissiere-Guerinet
 */
public class UniformResourceNameTest {

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_410")
    @Purpose("The SIP identifier is an URN")
    public void testFromStringSIP() {
        final OaisUniformResourceName sipUrn = new OaisUniformResourceName(OAISIdentifier.SIP,
                                                                           EntityType.COLLECTION,
                                                                           "CDPP",
                                                                           UUID.randomUUID(),
                                                                           1,
                                                                           null,
                                                                           null);
        final Pattern pattern = Pattern.compile(OaisUniformResourceName.URN_PATTERN);
        Assert.assertTrue(pattern.matcher(sipUrn.toString()).matches());
        Assert.assertTrue(OaisUniformResourceName.isValidUrn(sipUrn.toString()));
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_410")
    @Purpose("The AIP identifier is an URN")
    public void testFromStringFullAIP() {
        final OaisUniformResourceName aipUrn = new OaisUniformResourceName(OAISIdentifier.AIP,
                                                                           EntityType.COLLECTION,
                                                                           "CDPP",
                                                                           UUID.randomUUID(),
                                                                           1,
                                                                           2L,
                                                                           "3");
        final Pattern pattern = Pattern.compile(OaisUniformResourceName.URN_PATTERN);
        Assert.assertTrue(pattern.matcher(aipUrn.toString()).matches());
        Assert.assertTrue(OaisUniformResourceName.isValidUrn(aipUrn.toString()));
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_410")
    @Purpose("The AIP identifier is an URN")
    public void testFromStringAIPWithoutRevision() {
        final OaisUniformResourceName aipUrn = new OaisUniformResourceName(OAISIdentifier.AIP,
                                                                           EntityType.COLLECTION,
                                                                           "CDPP",
                                                                           UUID.randomUUID(),
                                                                           1,
                                                                           2L,
                                                                           null);
        final Pattern pattern = Pattern.compile(OaisUniformResourceName.URN_PATTERN);
        Assert.assertTrue(pattern.matcher(aipUrn.toString()).matches());
        Assert.assertTrue(OaisUniformResourceName.isValidUrn(aipUrn.toString()));
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_410")
    @Purpose("The AIP identifier is an URN")
    public void testFromStringAIPWithoutOrder() {
        final OaisUniformResourceName aipUrn = new OaisUniformResourceName(OAISIdentifier.AIP,
                                                                           EntityType.COLLECTION,
                                                                           "CDPP",
                                                                           UUID.randomUUID(),
                                                                           1,
                                                                           null,
                                                                           "revision");
        final Pattern pattern = Pattern.compile(OaisUniformResourceName.URN_PATTERN);
        Assert.assertTrue(pattern.matcher(aipUrn.toString()).matches());
        Assert.assertTrue(OaisUniformResourceName.isValidUrn(aipUrn.toString()));
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_410")
    @Purpose("The AIP identifier is an URN")
    public void testFromStringAIPWithoutOrderOrRevision() {
        final OaisUniformResourceName aipUrn = new OaisUniformResourceName(OAISIdentifier.AIP,
                                                                           EntityType.COLLECTION,
                                                                           "CDPP",
                                                                           UUID.randomUUID(),
                                                                           1,
                                                                           null,
                                                                           null);
        final Pattern pattern = Pattern.compile(OaisUniformResourceName.URN_PATTERN);
        Assert.assertTrue(pattern.matcher(aipUrn.toString()).matches());
        Assert.assertTrue(OaisUniformResourceName.isValidUrn(aipUrn.toString()));
    }

    @Test
    public void testFromString() {
        OaisUniformResourceName oaisUrn = OaisUniformResourceName.fromString(
            "URN:AIP:DATA:project1:9b702995-e2b9-393c-8a74-9b396db0edde:V1");
        Assert.assertEquals("AIP", oaisUrn.getIdentifier());
        Assert.assertEquals(EntityType.DATA, oaisUrn.getEntityType());
        Assert.assertEquals("project1", oaisUrn.getTenant());
        Assert.assertEquals(UUID.fromString("9b702995-e2b9-393c-8a74-9b396db0edde"), oaisUrn.getEntityId());
        Assert.assertEquals(new Integer(1), oaisUrn.getVersion());
        Assert.assertFalse(oaisUrn.isLast());
        Assert.assertNull(oaisUrn.getOrder());
        Assert.assertNull(oaisUrn.getRevision());
    }

}
