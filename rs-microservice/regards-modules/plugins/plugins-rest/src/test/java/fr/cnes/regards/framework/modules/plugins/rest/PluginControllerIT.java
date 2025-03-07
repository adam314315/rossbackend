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
package fr.cnes.regards.framework.modules.plugins.rest;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.event.AbstractPluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.domain.event.BroadcastPluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.CannotInstanciatePluginException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.plugins.service.PluginCache;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static fr.cnes.regards.framework.modules.plugins.domain.event.PluginServiceAction.CREATE;
import static fr.cnes.regards.framework.modules.plugins.domain.event.PluginServiceAction.UPDATE;

/**
 * Test plugin controller
 *
 * @author Marc Sordi
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=plugin_it",
                                   "regards.cipher.key-location=src/test/resources/testKey",
                                   "regards.cipher.iv=1234567812345678" })
public class PluginControllerIT extends AbstractRegardsTransactionalIT {

    private static final String REF_INNER_PLUGIN = "@ref@";

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuntimeTenantResolver resolver;

    @Autowired
    private IPluginConfigurationRepository repository;

    @Autowired
    private IPublisher publisher;

    @Captor
    private ArgumentCaptor<ISubscribable> recordsCaptor;

    @Autowired
    private PluginCache pluginCache;

    @After
    public void cleanUp() {
        resolver.forceTenant(getDefaultTenant());
        repository.deleteAll();
        // reset publisher count between tests
        Mockito.reset(publisher);
    }

    @Test
    public void savePluginConfigurationTest() throws ModuleException {

        // Bad version plugin creation attempt
        // Creation Inner plugin : must fail
        performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS,
                           readJsonContract("innerConfUpdatedVersion.json"),
                           customizer().expect(MockMvcResultMatchers.status().isUnprocessableEntity()),
                           "Configuration should be saved!",
                           "InnerParamTestPlugin");

        // Inner plugin creation
        ResultActions result = performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS,
                                                  readJsonContract("innerConf.json"),
                                                  customizer().expectStatusCreated(),
                                                  "Configuration should be saved!",
                                                  "InnerParamTestPlugin");
        String resultAsString = payload(result);
        String innerConfigId = JsonPath.read(resultAsString, "$.content.businessId");

        // Creation plugin with inner plugin as parameter
        String json = readJsonContract("fakeConf.json").replace(REF_INNER_PLUGIN, innerConfigId);
        result = performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS,
                                    json,
                                    customizer().expectStatusCreated(),
                                    "Configuration should be saved!",
                                    "ParamTestPlugin");
        resultAsString = payload(result);
        // Instanciate plugin
        resolver.forceTenant(getDefaultTenant());
        IParamTestPlugin plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class);
        IParamTestPlugin plugin2 = pluginService.getFirstPluginByType(IParamTestPlugin.class);
        Assert.assertNotNull(plugin);
        Assert.assertNotNull(plugin2);
        Assert.assertEquals("Should be the same instance ", plugin, plugin2);
        // Ensure plugins cached events are sent
        Mockito.verify(publisher, Mockito.atLeast(4)).publish(recordsCaptor.capture());
        // innerConf and fakeConf plugin names are:
        List<String> pluginLabels = Lists.newArrayList("sebbbbb", "Oliiiiiiive");
        pluginLabels.forEach(pluginLabel -> {
            Assert.assertTrue("plugin BroadcastPluginConfEvent exists",
                              getCaptureEvent(BroadcastPluginConfEvent.class, pluginLabel));
            Assert.assertTrue("plugin PluginConfEvent exists", getCaptureEvent(PluginConfEvent.class, pluginLabel));
        });
        // With dynamic parameter
        String dynValue = "toto";
        plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class,
                                                    IPluginParam.build("pString", dynValue).dynamic());
        Assert.assertNotNull(plugin);

        if (plugin instanceof ParamTestPlugin p) {
            Assert.assertEquals(p.getpString(), dynValue);
        } else {
            Assert.fail();
        }

        // With bad dynamic parameter
        boolean unexpectedValue = false;
        try {
            plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class,
                                                        IPluginParam.build("pString", "fake").dynamic());
        } catch (PluginUtilsRuntimeException e) {
            unexpectedValue = true;
        }
        Assert.assertTrue(unexpectedValue);

        // With integer dynamic parameter
        Integer dynInt = 10;
        plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class,
                                                    IPluginParam.build("pString", dynValue).dynamic(),
                                                    IPluginParam.build("pInteger", dynInt).dynamic());
        Assert.assertNotNull(plugin);
        if (plugin instanceof ParamTestPlugin p) {
            Assert.assertEquals(p.getpString(), dynValue);
            Assert.assertEquals(p.getpInteger(), dynInt);
        } else {
            Assert.fail();
        }

        // Update Inner Plugin
        json = readJsonContract("innerConfUpdated.json").replace(REF_INNER_PLUGIN, innerConfigId);
        performDefaultPut(PluginController.PLUGINS_PLUGINID_CONFIGID,
                          json,
                          customizer().expectStatusOk(),
                          "Configuration should be saved!",
                          "InnerParamTestPlugin",
                          innerConfigId);

        // Ensure plugins cached events are sent
        Mockito.verify(publisher, Mockito.atLeast(5)).publish(recordsCaptor.capture());
        List<BroadcastPluginConfEvent> broadcastPluginConfEvents = recordsCaptor.getAllValues()
                                                                                .stream()
                                                                                .filter(event -> event instanceof BroadcastPluginConfEvent)
                                                                                .map(BroadcastPluginConfEvent.class::cast) // now a Stream<SuitCard>
                                                                                .toList();
        Assert.assertEquals("should send 4 more create event",
                            4,
                            broadcastPluginConfEvents.stream().filter(bpce -> bpce.getAction() == CREATE).count());
        // Send the update event to the cache service
        List<BroadcastPluginConfEvent> updatePluginEvents = broadcastPluginConfEvents.stream()
                                                                                     .filter(bpce -> bpce.getAction()
                                                                                                     == UPDATE)
                                                                                     .toList();
        Assert.assertEquals("should send a single update event", 1, updatePluginEvents.size());
        // send the event to the plugin cache service
        updatePluginEvents.forEach(event -> pluginCache.cleanPluginRecursively(resolver.getTenant(),
                                                                               event.getPluginBusinnessId()));

        // Re-instanciate plugin
        resolver.forceTenant(getDefaultTenant());
        plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class);
        Assert.assertNotNull(plugin);
        if (plugin instanceof ParamTestPlugin p) {
            Assert.assertTrue(p.getInnerPlugin() instanceof InnerParamTestPlugin);
            Assert.assertEquals("Panthere", ((InnerParamTestPlugin) p.getInnerPlugin()).getToto());
        } else {
            Assert.fail();
        }

        // Try to delete inner configuration
        performDefaultDelete(PluginController.PLUGINS_PLUGINID_CONFIGID,
                             customizer().expect(MockMvcResultMatchers.status().isForbidden()),
                             "Configuration mustn't have been deleted",
                             "InnerParamTestPlugin",
                             innerConfigId);

        // Update Inner Plugin with a different version (2.0.0)
        json = readJsonContract("innerConfUpdatedVersion.json").replace(REF_INNER_PLUGIN, innerConfigId);
        performDefaultPut(PluginController.PLUGINS_PLUGINID_CONFIGID,
                          json,
                          customizer().expect(MockMvcResultMatchers.status().isUnprocessableEntity()),
                          "Configuration should be saved!",
                          "InnerParamTestPlugin",
                          innerConfigId);
    }

    private boolean getCaptureEvent(Class myClass, String pluginLabel) {
        return recordsCaptor.getAllValues()
                            .stream()
                            .filter(myClass::isInstance)
                            .map(AbstractPluginConfEvent.class::cast)
                            .anyMatch(e -> e.getLabel().equals(pluginLabel));
    }

    @Test(expected = CannotInstanciatePluginException.class)
    public void instantiatePluginConfigurationTest() throws ModuleException {
        // Inner plugin creation with version 1.0.0
        ResultActions result = performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS,
                                                  readJsonContract("innerConf.json"),
                                                  customizer().expectStatusCreated(),
                                                  "Configuration should be saved!",
                                                  "InnerParamTestPlugin");
        String resultAsString = payload(result);
        String innerBusinessId = JsonPath.read(resultAsString, "$.content.businessId");
        // Remove from cache
        resolver.forceTenant(getDefaultTenant());
        pluginService.cleanLocalPluginCache(innerBusinessId);

        // Retrieve PLugin Configuration
        PluginConfiguration pluginConf = pluginService.loadPluginConfiguration(innerBusinessId);

        // Change version
        pluginConf.setVersion("3.0.0");
        // Save like a piglet
        repository.save(pluginConf);

        // Try load it
        @SuppressWarnings("unused") InnerParamTestPlugin plugin = pluginService.getPlugin(pluginConf.getBusinessId());
    }

    @Test
    public void saveConfWithInvalidParameters() {
        // Inner plugin creation
        ResultActions result = performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS,
                                                  readJsonContract("innerConf.json"),
                                                  customizer().expectStatusCreated(),
                                                  "Configuration should be saved!",
                                                  "InnerParamTestPlugin");
        String resultAsString = payload(result);
        String innerConfigId = JsonPath.read(resultAsString, "$.content.businessId");

        // Creation plugin with inner plugin as parameter
        String json = readJsonContract("fakeConfInvalid.json").replace(REF_INNER_PLUGIN, innerConfigId);
        // Errors should be on each numerical value: pByte, pShort, pInteger, pLong
        // Error case on double and float is not tested because large float or double are interpreted as Infinity unless gson breaks.
        performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS,
                           json,
                           customizer().expectStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                                       .expectIsArray("$.messages")
                                       .expectToHaveSize("$.messages", 1),
                           "Configuration should not be saved!",
                           "ParamTestPlugin");
    }
}
