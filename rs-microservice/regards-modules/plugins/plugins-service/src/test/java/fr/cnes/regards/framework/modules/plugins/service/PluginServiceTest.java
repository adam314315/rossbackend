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

package fr.cnes.regards.framework.modules.plugins.service;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.IComplexInterfacePlugin;
import fr.cnes.regards.framework.modules.plugins.ISamplePlugin;
import fr.cnes.regards.framework.modules.plugins.SamplePlugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.event.BroadcastPluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginServiceAction;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;

/**
 * Unit testing of {@link PluginService}.
 *
 * @author Christophe Mertz
 * @author Sébastien Binda
 */
public class PluginServiceTest extends PluginServiceUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginServiceTest.class);

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_200")
    @Purpose("Load all plugin's metada.")
    public void getAllPlugins() {
        final List<PluginMetaData> metadaDatas = pluginServiceMocked.getPlugins();

        Assert.assertNotNull(metadaDatas);
        Assert.assertFalse(metadaDatas.isEmpty());

        LOGGER.debug("List all plugins :");
        metadaDatas.forEach(p -> LOGGER.debug(p.getPluginId()));
    }

    @Test
    public void getAllPluginTypes() {
        Set<String> types = pluginServiceMocked.getPluginTypes();

        Assert.assertNotNull(types);
        Assert.assertFalse(types.isEmpty());

        LOGGER.debug("List all plugin types :");
        types.forEach(LOGGER::debug);
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_200")
    @Purpose("Load all plugin's metada for a specific plugin type identified by a Class.")
    public void getPluginOneType() {
        final List<PluginMetaData> plugins = pluginServiceMocked.getPluginsByType(IComplexInterfacePlugin.class);

        Assert.assertNotNull(plugins);
        Assert.assertFalse(plugins.isEmpty());

        LOGGER.debug("List all plugins of type <IComplexInterfacePlugin.class> :");
        plugins.forEach(p -> LOGGER.debug(p.getPluginId()));
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_200")
    @Purpose("Load all plugin's metada for a specific plugin type identified by a class name.")
    public void getPluginTypesByString() {
        final String aClass = "fr.cnes.regards.framework.modules.plugins.IComplexInterfacePlugin";
        List<PluginMetaData> plugins = null;

        try {
            plugins = pluginServiceMocked.getPluginsByType(Class.forName(aClass));
        } catch (final ClassNotFoundException e) {
            Assert.fail();
        }

        Assert.assertNotNull(plugins);
        Assert.assertFalse(plugins.isEmpty());

        LOGGER.debug(String.format("List all plugins of type <%s> :", aClass));
        plugins.forEach(p -> LOGGER.debug(p.getPluginId()));
    }

    /**
     * Get a {@link PluginConfiguration}.
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_100")
    @Purpose("Get a plugin configuration identified by an identifier.")
    public void getAPluginConfiguration() {
        try {
            final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
            aPluginConfiguration.setId(AN_ID);
            Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
                   .thenReturn(aPluginConfiguration);
            Mockito.when(pluginDaoServiceMocked.existsByBusinessId(aPluginConfiguration.getBusinessId()))
                   .thenReturn(true);

            PluginConfiguration aConf;
            aConf = pluginServiceMocked.getPluginConfiguration(aPluginConfiguration.getBusinessId());
            Assert.assertEquals(aConf.getLabel(), aPluginConfiguration.getLabel());
        } catch (ModuleException e) {
            Assert.fail();
        }
    }

    /**
     * Delete a {@link PluginConfiguration}.
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_100")
    @Purpose("Delete a plugin configuration identified by an identifier")
    public void deleteAPluginConfiguration() {
        try {
            final PluginConfiguration pluginConf = getPluginConfigurationWithParameters();
            pluginConf.setId(AN_ID);
            //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
            pluginConf.setMetaDataAndPluginId(PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID));
            Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(pluginConf.getBusinessId()))
                   .thenReturn(pluginConf);
            pluginServiceMocked.deletePluginConfiguration(pluginConf.getBusinessId());
            Mockito.verify(pluginDaoServiceMocked).deleteById(pluginConf.getId());
            Mockito.verify(publisherMocked)
                   .publish(new BroadcastPluginConfEvent(pluginConf.getId(),
                                                         pluginConf.getBusinessId(),
                                                         pluginConf.getLabel(),
                                                         PluginServiceAction.DELETE,
                                                         pluginConf.getInterfaceNames()));

        } catch (final ModuleException e) {
            Assert.fail();
        }
    }

    private PluginConfiguration clone(PluginConfiguration source) {
        PluginConfiguration conf = new PluginConfiguration(source.getLabel(),
                                                           source.getParameters(),
                                                           source.getPriorityOrder(),
                                                           source.getPluginId());
        return conf;
    }

    /**
     * savePluginConfiguration a {@link PluginConfiguration}.
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_100")
    @Requirement("REGARDS_DSL_CMP_PLG_300")
    @Purpose("Create a new plugin configuration")
    public void savePluginConfigurationAPluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        final PluginConfiguration aPluginConfigurationWithId = clone(aPluginConfiguration);
        aPluginConfigurationWithId.setId(AN_ID);
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        aPluginConfigurationWithId.setMetaDataAndPluginId(PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID));
        try {
            Mockito.when(pluginDaoServiceMocked.savePluginConfiguration(aPluginConfiguration)).thenReturn(aPluginConfigurationWithId);

            final PluginConfiguration savePluginConfigurationdPluginConfiguration = pluginServiceMocked.savePluginConfiguration(
                aPluginConfiguration);

            Assert.assertEquals(aPluginConfiguration.getLabel(), savePluginConfigurationdPluginConfiguration.getLabel());
            Assert.assertEquals(aPluginConfiguration.getPluginId(), savePluginConfigurationdPluginConfiguration.getPluginId());
            Assert.assertEquals(aPluginConfiguration.isActive(), savePluginConfigurationdPluginConfiguration.isActive());
            Assert.assertEquals(aPluginConfiguration.getParameters().size(),
                                savePluginConfigurationdPluginConfiguration.getParameters().size());

            Mockito.verify(publisherMocked)
                   .publish(new BroadcastPluginConfEvent(aPluginConfigurationWithId.getId(),
                                                         aPluginConfigurationWithId.getBusinessId(),
                                                         aPluginConfigurationWithId.getLabel(),
                                                         PluginServiceAction.CREATE,
                                                         aPluginConfigurationWithId.getInterfaceNames()));
        } catch (final ModuleException e) {
            Assert.fail();
        }
    }

    @Test(expected = ModuleException.class)
    public void savePluginConfigurationTwoPluginConfigurationWithSameBusinessId() throws ModuleException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        final PluginConfiguration aPluginConfigurationWithId = clone(aPluginConfiguration);
        aPluginConfigurationWithId.setBusinessId(aPluginConfiguration.getBusinessId());
        aPluginConfigurationWithId.setId(AN_ID);
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        aPluginConfigurationWithId.setMetaDataAndPluginId(PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID));

        final PluginConfiguration theSamePluginConfiguration = getPluginConfigurationWithParameters();
        theSamePluginConfiguration.setBusinessId(aPluginConfiguration.getBusinessId());

        // savePluginConfiguration a first plugin configuration
        Mockito.when(pluginDaoServiceMocked.savePluginConfiguration(aPluginConfiguration)).thenReturn(aPluginConfigurationWithId);
        try {
            pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);
        } catch (ModuleException e) {
            Assert.fail();
        }

        // savePluginConfiguration a second plugin configuration with the same businessId
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(aPluginConfigurationWithId.getBusinessId()))
               .thenReturn(aPluginConfigurationWithId);
        pluginServiceMocked.savePluginConfiguration(theSamePluginConfiguration);

        // An exception is throw, otherwise the test is failed
        Assert.fail();
    }

    @Test
    public void savePluginConfigurationTwoPluginConfigurationWithSameLabel() throws ModuleException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        final PluginConfiguration aPluginConfigurationWithId = clone(aPluginConfiguration);
        aPluginConfigurationWithId.setId(AN_ID);
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        aPluginConfigurationWithId.setMetaDataAndPluginId(PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID));

        final PluginConfiguration theSamePluginConfiguration = getPluginConfigurationWithParameters();

        // savePluginConfiguration a first plugin configuration
        Mockito.when(pluginDaoServiceMocked.savePluginConfiguration(aPluginConfiguration)).thenReturn(aPluginConfigurationWithId);
        try {
            pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);
        } catch (ModuleException e) {
            Assert.fail();
        }

        // savePluginConfiguration a second plugin configuration with the same label
        pluginServiceMocked.savePluginConfiguration(theSamePluginConfiguration);
    }

    /**
     * Update a {@link PluginConfiguration}.
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_100")
    @Requirement("REGARDS_DSL_CMP_PLG_100")
    @Purpose("Update a plugin configuration identified by an identifier")
    public void updateAPluginConfiguration() throws ModuleException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(aPluginConfiguration);
        Mockito.when(pluginDaoServiceMocked.savePluginConfiguration(aPluginConfiguration)).thenReturn(aPluginConfiguration);

        final PluginConfiguration updatedConf = pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);
        Assert.assertEquals(updatedConf.getLabel(), aPluginConfiguration.getLabel());
        Assert.assertEquals(updatedConf.getPluginId(), aPluginConfiguration.getPluginId());
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_100")
    @Requirement("REGARDS_DSL_CMP_PLG_100")
    @Purpose("Update a plugin configuration identified by an identifier by deactivating it")
    public void deactivateAPluginConfiguration() throws ModuleException {
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        PluginMetaData metaData = PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID);
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
        aPluginConfiguration.setMetaDataAndPluginId(metaData);
        aPluginConfiguration.setVersion(metaData.getVersion());
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(aPluginConfiguration);
        PluginConfiguration toBeUpdated = clone(aPluginConfiguration);
        toBeUpdated.setIsActive(false);
        toBeUpdated.setBusinessId(aPluginConfiguration.getBusinessId());
        toBeUpdated.setId(AN_ID);
        Mockito.when(pluginDaoServiceMocked.savePluginConfiguration(toBeUpdated)).thenReturn(toBeUpdated);

        final PluginConfiguration updatedConf = pluginServiceMocked.updatePluginConfiguration(toBeUpdated);
        Assert.assertEquals(updatedConf.getLabel(), aPluginConfiguration.getLabel());
        Assert.assertEquals(updatedConf.getPluginId(), aPluginConfiguration.getPluginId());
        Mockito.verify(publisherMocked)
               .publish(new BroadcastPluginConfEvent(aPluginConfiguration.getId(),
                                                     aPluginConfiguration.getBusinessId(),
                                                     aPluginConfiguration.getLabel(),
                                                     PluginServiceAction.UPDATE,
                                                     aPluginConfiguration.getInterfaceNames()));
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_100")
    @Requirement("REGARDS_DSL_CMP_PLG_100")
    @Purpose("Update a plugin configuration identified by an identifier by activating it")
    public void activateAPluginConfiguration() throws ModuleException {
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        PluginMetaData metaData = PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID);
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
        aPluginConfiguration.setIsActive(false);
        aPluginConfiguration.setMetaDataAndPluginId(metaData);
        aPluginConfiguration.setVersion(metaData.getVersion());
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(aPluginConfiguration);
        PluginConfiguration toBeUpdated = clone(aPluginConfiguration);
        toBeUpdated.setBusinessId(aPluginConfiguration.getBusinessId());
        toBeUpdated.setIsActive(true);
        toBeUpdated.setId(AN_ID);
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        toBeUpdated.setMetaDataAndPluginId(PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID));
        Mockito.when(pluginDaoServiceMocked.savePluginConfiguration(toBeUpdated)).thenReturn(toBeUpdated);

        final PluginConfiguration updatedConf = pluginServiceMocked.updatePluginConfiguration(toBeUpdated);
        Assert.assertEquals(updatedConf.getLabel(), aPluginConfiguration.getLabel());
        Assert.assertEquals(updatedConf.getPluginId(), aPluginConfiguration.getPluginId());
        Mockito.verify(publisherMocked)
               .publish(new BroadcastPluginConfEvent(aPluginConfiguration.getId(),
                                                     aPluginConfiguration.getBusinessId(),
                                                     aPluginConfiguration.getLabel(),
                                                     PluginServiceAction.UPDATE,
                                                     aPluginConfiguration.getInterfaceNames()));
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_200")
    @Purpose("Load a plugin's metada for a specific plugin type identified by a plugin identifier.")
    public void getPluginMetaDataById() {
        final PluginMetaData pluginMetaData = pluginServiceMocked.getPluginMetaDataById("aSamplePlugin");
        Assert.assertNotNull(pluginMetaData);
        Assert.assertNotNull(pluginMetaData.getAuthor());
        Assert.assertNotNull(pluginMetaData.getVersion());
        Assert.assertNotNull(pluginMetaData.getDescription());
    }

    @Test
    public void getPluginConfigurationsByTypeWithClass() {
        // Given
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        pluginConfigurations.add(getPluginConfigurationWithDynamicParameter());
        pluginConfigurations.add(getPluginConfigurationWithParameters());

        Mockito.when(pluginDaoServiceMocked.findAllPluginConfigurations()).thenReturn(pluginConfigurations);
        // When
        List<PluginConfiguration> results = pluginServiceMocked.getPluginConfigurationsByType(ISamplePlugin.class);
        // Then
        Assert.assertNotNull(results);
        Assert.assertEquals(pluginConfigurations.size(), results.size());
        Assert.assertEquals(getPluginConfigurationWithParameters().getLabel(), results.get(0).getLabel());
        Assert.assertEquals(getPluginConfigurationWithDynamicParameter().getLabel(), results.get(1).getLabel());
    }

    @Test
    public void getPluginConfigurationsByTypeWithPluginId() {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        pluginConfs.add(getPluginConfigurationWithParameters());
        pluginConfs.add(getPluginConfigurationWithDynamicParameter());
        Mockito.when(pluginDaoServiceMocked.findByPluginIdOrderByPriorityOrderDesc(PLUGIN_PARAMETER_ID))
               .thenReturn(pluginConfs);
        final List<PluginConfiguration> results = pluginServiceMocked.getPluginConfigurations(PLUGIN_PARAMETER_ID);

        Assert.assertNotNull(results);
        Assert.assertEquals(pluginConfs.size(), results.size());
    }

    @Test
    public void getAllPluginConfigurations() {
        // Given
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        pluginConfigurations.add(getPluginConfigurationWithDynamicParameter());
        pluginConfigurations.add(getPluginConfigurationWithParameters());

        Mockito.when(pluginDaoServiceMocked.findAllPluginConfigurationsSorted(any())).thenReturn(pluginConfigurations);
        // When
        List<PluginConfiguration> results = pluginServiceMocked.getAllPluginConfigurations();
        // Then
        Assert.assertNotNull(results);
        Assert.assertEquals(pluginConfigurations.size(), results.size());
    }

    /**
     * Get the first plugin of a specific type
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_120")
    @Purpose("Load a plugin from a specific type with a configuration and execute a method.")
    public void getFirstPluginInstanceByType() throws ModuleException, NotAvailablePluginConfigurationException {
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        PluginMetaData metaData = PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID);
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
        aPluginConfiguration.setMetaDataAndPluginId(metaData);
        aPluginConfiguration.setVersion(metaData.getVersion());

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithDynamicParameter());

        Mockito.when(pluginDaoServiceMocked.findAllPluginConfigurations()).thenReturn(pluginConfs);
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(aPluginConfiguration);
        Mockito.when(pluginDaoServiceMocked.existsByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(true);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);

        Assert.assertNotNull(aSamplePlugin);

        final int result = aSamplePlugin.add(QUATRE, CINQ);
        LOGGER.debug(RESULT + result);
        Assert.assertTrue(result > 0);
        Assert.assertTrue(aSamplePlugin.echo(HELLO).contains(HELLO));
    }

    @Test
    public void getAPluginInstance() throws ModuleException, NotAvailablePluginConfigurationException {
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        PluginMetaData metaData = PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID);
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
        aPluginConfiguration.setMetaDataAndPluginId(metaData);
        aPluginConfiguration.setVersion(metaData.getVersion());
        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithDynamicParameter());

        Mockito.when(pluginDaoServiceMocked.findByPluginIdOrderByPriorityOrderDesc(PLUGIN_PARAMETER_ID))
               .thenReturn(pluginConfs);
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(aPluginConfiguration);
        Mockito.when(pluginDaoServiceMocked.existsByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(true);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getPlugin(aPluginConfiguration.getBusinessId());

        Assert.assertNotNull(aSamplePlugin);

        final int result = aSamplePlugin.add(QUATRE, CINQ);
        LOGGER.debug(RESULT + result);
        Assert.assertTrue(result > 0);
        Assert.assertTrue(aSamplePlugin.echo(HELLO).contains(HELLO));
    }

    /**
     * Get twice a specific Plugin with the same PluginConfiguration
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    @Purpose("Load twice a plugin with the same configuration.")
    public void getExistingFirstPluginInstanceByType()
        throws ModuleException, NotAvailablePluginConfigurationException {
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        PluginMetaData metaData = PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID);
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
        aPluginConfiguration.setMetaDataAndPluginId(metaData);
        aPluginConfiguration.setVersion(metaData.getVersion());

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithDynamicParameter());

        Mockito.when(pluginDaoServiceMocked.findAllPluginConfigurations()).thenReturn(pluginConfs);
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(aPluginConfiguration);
        Mockito.when(pluginDaoServiceMocked.existsByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(true);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);
        Assert.assertNotNull(aSamplePlugin);

        final SamplePlugin bSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);
        Assert.assertNotNull(bSamplePlugin);

        Assert.assertEquals(aSamplePlugin.add(5, 3), bSamplePlugin.add(5, 3));
        Assert.assertEquals(aSamplePlugin.echo(GREEN), bSamplePlugin.echo(GREEN));

        Assert.assertEquals(aSamplePlugin, bSamplePlugin);
    }

    /**
     * Get twice a specific Plugin with the same PluginConfiguration with a dynamic parameter
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    @Purpose("Load a plugin twice from a specific type with a configuration.")
    public void getExistingFirstPluginInstanceByTypeWithDynamicParameter()
        throws ModuleException, NotAvailablePluginConfigurationException {
        // Given
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        PluginMetaData pluginMetadata = PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID);
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        PluginConfiguration pluginConfiguration0 = getPluginConfigurationWithDynamicParameter();
        pluginConfiguration0.setId(AN_ID);
        pluginConfiguration0.setMetaDataAndPluginId(pluginMetadata);
        pluginConfiguration0.setVersion(pluginMetadata.getVersion());
        pluginConfigurations.add(pluginConfiguration0);

        PluginConfiguration pluginConfiguration1 = getPluginConfigurationWithParameters();
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        pluginConfiguration1.setMetaDataAndPluginId(pluginMetadata);
        pluginConfiguration1.setVersion(pluginMetadata.getVersion());
        pluginConfiguration1.setPriorityOrder(10);
        pluginConfigurations.add(pluginConfiguration1);

        Mockito.when(pluginDaoServiceMocked.findAllPluginConfigurations()).thenReturn(pluginConfigurations);
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(pluginConfiguration0.getBusinessId()))
               .thenReturn(pluginConfiguration0);
        Mockito.when(pluginDaoServiceMocked.existsByBusinessId(pluginConfiguration0.getBusinessId()))
               .thenReturn(true);

        // the argument for the dynamic parameter
        IPluginParam aDynamicPlgParam = IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX, BLUE).dynamic();

        // When
        SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class, aDynamicPlgParam);
        SamplePlugin bSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class, aDynamicPlgParam);
        // Then
        Assert.assertNotNull(aSamplePlugin);
        Assert.assertNotNull(bSamplePlugin);
        Assert.assertNotEquals(aSamplePlugin, bSamplePlugin);
    }

    /**
     * Get twice a specific Plugin with the same PluginConfiguration with a dynamic parameter the second time
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    @Purpose("Load a plugin twice from a specific type with a configuration.")
    public void getExistingFirstPluginInstanceByTypeWithDynamicParameter2()
        throws ModuleException, NotAvailablePluginConfigurationException {
        // Given
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        PluginMetaData pluginMetadata = PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID);
        final List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        final PluginConfiguration pluginConfiguration0 = getPluginConfigurationWithDynamicParameter();
        pluginConfiguration0.setId(AN_ID);
        pluginConfiguration0.setMetaDataAndPluginId(pluginMetadata);
        pluginConfiguration0.setVersion(pluginMetadata.getVersion());
        pluginConfigurations.add(pluginConfiguration0);

        PluginConfiguration pluginConfiguration1 = getPluginConfigurationWithParameters();
        pluginConfiguration1.setPriorityOrder(10);
        pluginConfigurations.add(pluginConfiguration1);

        Mockito.when(pluginDaoServiceMocked.findAllPluginConfigurations()).thenReturn(pluginConfigurations);
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(pluginConfiguration0.getBusinessId()))
               .thenReturn(pluginConfiguration0);

        // the argument for the dynamic parameter
        IPluginParam aDynamicPlgParam = IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX, BLUE).dynamic();

        // When
        SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);
        SamplePlugin bSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class, aDynamicPlgParam);
        // Then
        Assert.assertNotNull(aSamplePlugin);
        Assert.assertNotNull(bSamplePlugin);
        Assert.assertNotEquals(aSamplePlugin, bSamplePlugin);
    }

    /**
     * Get the first plugin of a specific type with a specific parameter
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_120")
    @Requirement("REGARDS_DSL_CMP_PLG_120")
    @Requirement("REGARDS_DSL_CMP_PLG_300")
    @Requirement("REGARDS_DSL_CMP_PLG_320")
    @Requirement("REGARDS_DSL_CMP_PLG_340")
    @Purpose("Load a plugin with a dynamic parameter from a specific type with a configuration and execute a method.")
    public void getFirstPluginInstanceByTypeWithADynamicParameter()
        throws ModuleException, NotAvailablePluginConfigurationException {
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        PluginMetaData metaData = PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID);
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
        aPluginConfiguration.setMetaDataAndPluginId(metaData);
        aPluginConfiguration.setVersion(metaData.getVersion());

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithDynamicParameter());

        Mockito.when(pluginDaoServiceMocked.findAllPluginConfigurations()).thenReturn(pluginConfs);
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(aPluginConfiguration);

        // the argument for the dynamic parameter
        IPluginParam aDynamicPlgParam = IPluginParam.build(SamplePlugin.FIELD_NAME_COEF, -1).dynamic();

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class,
                                                                                    aDynamicPlgParam);

        Assert.assertNotNull(aSamplePlugin);

        final int result = aSamplePlugin.add(QUATRE, CINQ);
        LOGGER.debug(RESULT + result);

        Assert.assertTrue(result < 0);
        Assert.assertTrue(aSamplePlugin.echo(HELLO).contains(HELLO));
    }

    /**
     * Get the first plugin of a specific type with a dynamic parameter. Used the default value for the dynamic
     * parameter.
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_120")
    @Requirement("REGARDS_DSL_CMP_PLG_300")
    @Requirement("REGARDS_DSL_CMP_PLG_340")
    @Purpose(
        "Load a plugin with a dynamic parameter with a list of value from a specific type with a configuration and execute a method.")
    public void getFirstPluginInstanceByTypeWithADynamicParameterWithAListOfValue()
        throws ModuleException, NotAvailablePluginConfigurationException {
        // Given
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        PluginMetaData pluginMetadata = PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID);
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        PluginConfiguration pluginConfiguration0 = getPluginConfigurationWithDynamicParameter();
        pluginConfiguration0.setId(AN_ID);
        pluginConfiguration0.setMetaDataAndPluginId(pluginMetadata);
        pluginConfiguration0.setVersion(pluginMetadata.getVersion());
        pluginConfigurations.add(pluginConfiguration0);

        PluginConfiguration pluginConfiguration1 = getPluginConfigurationWithDynamicParameter();
        pluginConfiguration1.setPriorityOrder(10);
        pluginConfigurations.add(pluginConfiguration1);

        Mockito.when(pluginDaoServiceMocked.findAllPluginConfigurations()).thenReturn(pluginConfigurations);
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(pluginConfiguration0.getBusinessId()))
               .thenReturn(pluginConfiguration0);
        Mockito.when(pluginDaoServiceMocked.existsByBusinessId(pluginConfiguration0.getBusinessId()))
               .thenReturn(true);
        // When
        SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);
        // Then
        Assert.assertNotNull(aSamplePlugin);
        Assert.assertTrue(aSamplePlugin.echo(HELLO).contains(RED));
    }

    /**
     * Get the first plugin of a specific type with a dynamic parameter. Set a value for the dynamic parameter.
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_120")
    @Requirement("REGARDS_DSL_CMP_PLG_300")
    @Requirement("REGARDS_DSL_CMP_PLG_340")
    @Purpose(
        "Load a plugin with a dynamic parameter with a list of value from a specific type with a configuration and set a parameter value and execute a method.")
    public void getFirstPluginInstanceByTypeWithADynamicParameterWithAListOfValueAndSetAValue()
        throws ModuleException, NotAvailablePluginConfigurationException {
        // Given
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        PluginMetaData pluginMetadata = PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID);
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        PluginConfiguration pluginConfiguration0 = getPluginConfigurationWithDynamicParameter();
        pluginConfiguration0.setId(AN_ID);
        pluginConfiguration0.setMetaDataAndPluginId(pluginMetadata);
        pluginConfiguration0.setVersion(pluginMetadata.getVersion());
        pluginConfigurations.add(pluginConfiguration0);

        PluginConfiguration pluginConfiguration1 = getPluginConfigurationWithParameters();
        pluginConfiguration1.setPriorityOrder(10);
        pluginConfigurations.add(pluginConfiguration1);

        Mockito.when(pluginDaoServiceMocked.findAllPluginConfigurations()).thenReturn(pluginConfigurations);
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(pluginConfiguration0.getBusinessId()))
               .thenReturn(pluginConfiguration0);
        Mockito.when(pluginDaoServiceMocked.existsByBusinessId(pluginConfiguration0.getBusinessId()))
               .thenReturn(true);

        // the argument for the dynamic parameter
        IPluginParam aDynamicPlgParam = IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX, BLUE).dynamic();

        // When
        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class,
                                                                                    aDynamicPlgParam);
        // Then
        Assert.assertNotNull(aSamplePlugin);
        Assert.assertTrue(aSamplePlugin.echo(HELLO).contains(BLUE));
    }

    /**
     * Try to get a plugin of a specific type with a dynamic parameter BUT a bad version.
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test(expected = CannotInstanciatePluginException.class)
    public void getAPluginInstanceWithBadVersionConfiguration()
        throws ModuleException, NotAvailablePluginConfigurationException {
        // Given
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        PluginConfiguration pluginConfiguration0 = getPluginConfigurationWithDynamicParameter();
        pluginConfiguration0.setVersion(BLUE);
        pluginConfiguration0.setId(AN_ID);
        pluginConfigurations.add(pluginConfiguration0);

        PluginConfiguration pluginConfiguration1 = getPluginConfigurationWithParameters();
        pluginConfiguration1.setPriorityOrder(10);
        pluginConfigurations.add(pluginConfiguration1);

        Mockito.when(pluginDaoServiceMocked.findAllPluginConfigurations()).thenReturn(pluginConfigurations);
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(pluginConfiguration0.getBusinessId()))
               .thenReturn(pluginConfiguration0);
        Mockito.when(pluginDaoServiceMocked.existsByBusinessId(pluginConfiguration0.getBusinessId()))
               .thenReturn(true);
        // When
        pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);
    }

    /**
     * Get the first plugin with the configuration the most priority.
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    public void getFirstPluginInstanceTheMostPrioritary()
        throws ModuleException, NotAvailablePluginConfigurationException {
        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        PluginMetaData metaData = PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID);
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();

        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        // this conf is the most priority
        aPluginConfiguration.setPriorityOrder(1);
        aPluginConfiguration.setId(AN_ID);
        aPluginConfiguration.setMetaDataAndPluginId(metaData);
        aPluginConfiguration.setVersion(metaData.getVersion());

        final PluginConfiguration bPluginConfiguration = getPluginConfigurationWithParameters();
        bPluginConfiguration.setPriorityOrder(2);
        bPluginConfiguration.setId(1 + AN_ID);
        bPluginConfiguration.setMetaDataAndPluginId(metaData);
        bPluginConfiguration.setVersion(metaData.getVersion());

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(bPluginConfiguration);

        Mockito.when(pluginDaoServiceMocked.findAllPluginConfigurations()).thenReturn(pluginConfs);
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(aPluginConfiguration);
        Mockito.when(pluginDaoServiceMocked.existsByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(true);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);

        Assert.assertNotNull(aSamplePlugin);
        Assert.assertTrue(aSamplePlugin.echo(HELLO).contains(RED));
    }

    @Test
    public void checkPluginName() throws EntityInvalidException {
        String className = "fr.cnes.regards.framework.modules.plugins.SamplePlugin";
        PluginMetaData metaData = pluginServiceMocked.checkPluginClassName(ISamplePlugin.class, className);
        Assert.assertNotNull(metaData);
        Assert.assertEquals(className, metaData.getPluginClassName());
    }

    @Test(expected = EntityInvalidException.class)
    public void checkPluginNameFailed() throws EntityInvalidException {
        String className = "fr.cnes.regards.framework.plugins.SmplePlugin";
        pluginServiceMocked.checkPluginClassName(ISamplePlugin.class, className);
        Assert.fail();
    }

}
