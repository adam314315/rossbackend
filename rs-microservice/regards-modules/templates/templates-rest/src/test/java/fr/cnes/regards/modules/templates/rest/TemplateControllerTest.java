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
package fr.cnes.regards.modules.templates.rest;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import fr.cnes.regards.modules.templates.test.TemplateTestConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDecisionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Templates controller unit test
 *
 * @author Xavier-Alexandre Brochard
 */
public class TemplateControllerTest {

    /**
     * A template with some values
     */
    private Template template;

    /**
     * Controller under test
     */
    private TemplateController templateController;

    /**
     * Mocked template service
     */
    private ITemplateService templateService;

    @Before
    public void setUp() {
        // Init a template
        template = new Template(TemplateTestConstants.CODE, TemplateTestConstants.CONTENT);
        template.setId(TemplateTestConstants.ID);

        // Mock stuff
        templateService = Mockito.mock(ITemplateService.class);
        AccessDecisionManager accessDecisionManager = Mockito.mock(AccessDecisionManager.class);

        IResourceService resourceService = new MockDefaultResourceService(accessDecisionManager);

        // Instanciate the tested class
        templateController = new TemplateController(templateService, resourceService);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#findAll()}.
     */
    @Test
    @Purpose("Check that the system allows to retrieve the list of all templates.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testFindAll() {
        // Mock service
        final List<Template> templates = Collections.singletonList(template);
        Mockito.when(templateService.findAll()).thenReturn(templates);

        // Define actual
        final ResponseEntity<List<EntityModel<Template>>> actual = templateController.findAll();

        // Check
        Assert.assertEquals(template.getName(), actual.getBody().get(0).getContent().getName());
        Assert.assertEquals(template.getContent(), actual.getBody().get(0).getContent().getContent());
        Mockito.verify(templateService).findAll();
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#findById(java.lang.Long)}.
     *
     * @throws EntityNotFoundException if no template with passed id could be found
     */
    @Test
    @Purpose("Check that the system allows to retrieve a single template.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testFindById() throws EntityNotFoundException {
        // Mock service
        Mockito.when(templateService.findById(TemplateTestConstants.ID)).thenReturn(template);

        // Define actual
        final ResponseEntity<EntityModel<Template>> actual = templateController.findById(TemplateTestConstants.ID);

        // Check
        Assert.assertEquals(template.getName(), actual.getBody().getContent().getName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#findById(java.lang.Long)}.
     *
     * @throws EntityNotFoundException if no template with passed id could be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Purpose("Check that the system handles the case where trying to retrieve a template of unknown id.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testFindByIdNotFound() throws EntityNotFoundException {
        // Mock service
        Mockito.when(templateService.findById(TemplateTestConstants.ID)).thenThrow(EntityNotFoundException.class);

        // Trigger exception
        templateController.findById(TemplateTestConstants.ID);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.templates.rest.TemplateController#update(java.lang.Long, fr.cnes.regards.modules.templates.domain.Template)}.
     *
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} if no template with passed id could be found<br>
     *                         {@link EntityInconsistentIdentifierException} if the path id differs from the template id<br>
     */
    @Test
    @Purpose("Check that the system allows to update a template.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testUpdate() throws EntityException {
        // Call tested method
        templateController.update(TemplateTestConstants.ID, template);

        // Check that the repository's method was called with right arguments
        Mockito.verify(templateService).update(Mockito.anyLong(), Mockito.refEq(template));

    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.templates.rest.TemplateController#update(java.lang.Long, fr.cnes.regards.modules.templates.domain.Template)}.
     *
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} if no template with passed id could be found<br>
     *                         {@link EntityInconsistentIdentifierException} if the path id differs from the template id<br>
     */
    @Test(expected = EntityNotFoundException.class)
    @Purpose("Check that the system handles the case of updating an not existing template.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testUpdateNotFound() throws EntityException {
        // Mock
        Mockito.doThrow(EntityNotFoundException.class).when(templateService).update(TemplateTestConstants.ID, template);

        // Trigger exception
        templateController.update(TemplateTestConstants.ID, template);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.templates.rest.TemplateController#update(java.lang.Long, fr.cnes.regards.modules.templates.domain.Template)}.
     *
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} if no template with passed id could be found<br>
     *                         {@link EntityInconsistentIdentifierException} if the path id differs from the template id<br>
     */
    @Test(expected = EntityInconsistentIdentifierException.class)
    @Purpose("Check that the system allows the case of inconsistency of ids in the request.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testUpdateInconsistentIdentifier() throws EntityException {
        // Mock
        Mockito.doThrow(EntityInconsistentIdentifierException.class)
               .when(templateService)
               .update(TemplateTestConstants.ID, template);

        // Trigger exception
        templateController.update(TemplateTestConstants.ID, template);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.templates.rest.TemplateController#toResource(fr.cnes.regards.modules.templates.domain.Template, java.lang.Object[])}.
     */
    @Test
    public final void testToResource() {
        // Define expected
        template.setId(TemplateTestConstants.ID);
        final List<Link> links = new ArrayList<>();
        links.add(Link.of("/templates/" + TemplateTestConstants.ID, "self"));
        links.add(Link.of("/templates/" + TemplateTestConstants.ID, "update"));
        final EntityModel<Template> expected = EntityModel.of(template, links);

        // Define actual
        final EntityModel<Template> actual = templateController.toResource(template);

        // Check
        Assert.assertEquals(expected.getContent().getId(), actual.getContent().getId());
        Assert.assertEquals(expected.getContent().getName(), actual.getContent().getName());
        Assert.assertEquals(expected.getContent().getContent(), actual.getContent().getContent());
        Assert.assertEquals(expected.getLinks(), actual.getLinks());
    }

}
