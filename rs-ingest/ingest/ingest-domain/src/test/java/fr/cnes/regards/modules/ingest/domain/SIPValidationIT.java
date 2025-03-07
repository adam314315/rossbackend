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
package fr.cnes.regards.modules.ingest.domain;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.oais.dto.InformationPackageProperties;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPReference;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dto.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.StorageDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeType;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.HashMap;

/**
 * {@link SIPDto} and {@link SIPCollection} validation tests
 *
 * @author Marc Sordi
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ValidationAutoConfiguration.class)
@TestPropertySource(properties = { "regards.cipher.iv=1234567812345678",
                                   "regards.cipher.keyLocation=src/test/resources/testKey" })
@ActiveProfiles("test")
public class SIPValidationIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPValidationIT.class);

    private static final String PROVIDER_ID = "providerId";

    @Autowired
    private Validator validator;

    private Errors errors;

    @Before
    public void before() {
        errors = new MapBindingResult(new HashMap<>(), "sip");
    }

    @After
    public void logErrors() {
        errors.getAllErrors().forEach(error -> {
            LOGGER.debug("Validation ERROR ----------------> " + error.toString());
        });
    }

    /**
     * Check that SIP has to be passed either by reference or by regards. See {@link SIPDto} type constraints.
     */
    @Test
    @Requirement("REGARDS_DSL_ING_PRO_130")
    @Purpose("SIP validation")
    public void emptySIP() {

        SIPDto sip = new SIPDto();
        sip.setIpType(EntityType.DATA);

        validator.validate(sip, errors);
        if (!errors.hasErrors()) {
            Assert.fail("An empty SIP should be invalid");
        }
        Assert.assertEquals(1, errors.getErrorCount());
    }

    /**
     * Check validation on SIP passed by reference. See {@link SIPReference} for constraint list.
     */
    @Test
    @Requirement("REGARDS_DSL_ING_PRO_140")
    @Purpose("SIP validation")
    public void invalidSIPReference() {

        SIPDto sip = new SIPDto();
        sip.setId(PROVIDER_ID);
        sip.setIpType(EntityType.DATA);
        SIPReference ref = new SIPReference();
        sip.setRef(ref);

        validator.validate(sip, errors);
        if (!errors.hasErrors()) {
            Assert.fail("An empty SIP reference should be invalid");
        }
        Assert.assertEquals(5, errors.getErrorCount());
    }

    /**
     * Check that builder properly build a SIP reference. See {@link #invalidSIPReference()} for required properties.
     */
    @Test
    @Requirement("REGARDS_DSL_ING_PRO_140")
    @Purpose("SIP validation")
    public void validSIPReference() {

        SIPDto sip = SIPDto.buildReference(EntityType.DATA,
                                           PROVIDER_ID,
                                           Paths.get("sip.xml"),
                                           "c26f20f5ef4453400b47fc98c3ce86b3");
        validator.validate(sip, errors);
        if (errors.hasErrors()) {
            Assert.fail("Builder should properly build SIP reference");
        }
    }

    /**
     * Check validation on SIP passed by value. See {@link InformationPackageProperties} for constraint list.
     */
    @Ignore("A Sip can have no data file (cf. M.Sordi, 18/09/2018)")
    @Test
    @Requirement("REGARDS_DSL_ING_PRO_140")
    @Purpose("SIP validation")
    public void invalidSIPValue() {

        SIPDto sip = new SIPDto();
        sip.setId(PROVIDER_ID);
        sip.setIpType(EntityType.DATA);
        sip.setProperties(new InformationPackageProperties());

        validator.validate(sip, errors);
        if (!errors.hasErrors()) {
            Assert.fail("An empty SIP value should be invalid");
        }
        // Missing at least one content information
        Assert.assertTrue(errors.getErrorCount() == 1);
    }

    /**
     * Check that builder properly build a SIP value.
     */
    @Test
    @Requirement("REGARDS_DSL_ING_PRO_140")
    @Purpose("SIP validation")
    public void validSIPValue() {

        SIPDto sip = SIPDto.build(EntityType.DATA, PROVIDER_ID);

        // Geometry
        sip.withGeometry(IGeometry.point(IGeometry.position(10.0, 10.0)));
        // Content information
        // Content information - data object
        sip.withDataObject(DataType.RAWDATA, Paths.get("sip.fits"), RandomChecksumUtils.generateRandomChecksum());
        // Content information - data object representation information
        sip.withSyntaxAndSemantic("FITS",
                                  "http://www.iana.org/assignments/media-types/application/fits",
                                  MimeType.valueOf("application/fits"),
                                  "semanticDescription");
        // Effectively add content information to the current SIP
        sip.registerContentInformation();

        // PDI
        // PDI - context information
        sip.withContextTags("CNES", "TOULOUSE", "FRANCE");
        sip.withContextInformation("CNES", "http://www.cnes.fr");
        // PDI - reference information
        sip.withReferenceInformation("doi", "dfdg://Dsfd.;");
        // PDI - provenance information
        sip.withFacility("facility");
        sip.withInstrument("instrument");
        sip.withFilter("filter");
        sip.withDetector("detector");
        sip.withProposal("proposal");
        sip.withAdditionalProvenanceInformation("from", "ender");
        // PDI - provenance information events
        sip.withProvenanceInformationEvent("SIP initialization");
        sip.withProvenanceInformationEvent("SIP_CREATION", "SIP creation", OffsetDateTime.now());
        // PDI - fixity
        sip.withFixityInformation("fixity", "see content info. for checksum algorithm");
        // PDI - access right information
        sip.withAccessRightInformation("MIT", "public", OffsetDateTime.now());

        validator.validate(sip, errors);
        if (errors.hasErrors()) {
            Assert.fail("Builder should properly build SIP value");
        }
    }

    /**
     * Check that {@link IngestMetadataDto} is properly validated. Empty collection is accepted!
     */
    @Test
    public void validateSIPCollection() {

        IngestMetadataDto metadata = new IngestMetadataDto("sessionOwner",
                                                           "session",
                                                           null,
                                                           "ingestChain",
                                                           Sets.newHashSet("cat 1"),
                                                           null,
                                                           null,
                                                           new StorageDto("test"));
        SIPCollection collection = SIPCollection.build(metadata);

        validator.validate(collection, errors);
        if (errors.hasErrors()) {
            Assert.fail("Builder should properly build SIP collection ");
        }
    }
}
