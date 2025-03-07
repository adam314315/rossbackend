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
package fr.cnes.regards.framework.modules.jobs.service;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.RandomFloatJob;
import fr.cnes.regards.framework.modules.jobs.domain.WaiterJob;
import fr.cnes.regards.framework.modules.jobs.test.MultiJobServiceConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * This test permits to test several job services locking mechanism.
 * IT IS MANDATORY TO COMMENT JobInitializer.onApplicationEvent() method to avoid launching jobInfoService.manage()
 * asynchronous method.
 *
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { MultiJobServiceConfiguration.class })
@DirtiesContext
@Ignore
@ActiveProfiles("test")
public class MultiJobServiceIT {

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private ITestJobInfoService testJobInfoService;

    @Autowired
    private IJobInfoRepository repository;

    @Value("${regards.tenant}")
    private String tenant;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    private JobInfo waitJobInfo;

    private JobInfo randomFloatJob;

    @Before
    public void tearUp() {
        repository.deleteAll();

        waitJobInfo = new JobInfo(false);
        waitJobInfo.setPriority(10);
        waitJobInfo.setClassName(WaiterJob.class.getName());
        waitJobInfo.setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, 500L),
                                  new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, 1));
        waitJobInfo = jobInfoService.createAsQueued(waitJobInfo);

        randomFloatJob = new JobInfo(false);
        randomFloatJob.setClassName(RandomFloatJob.class.getName());
        randomFloatJob = jobInfoService.createAsQueued(randomFloatJob);
    }

    @After
    public void clean() {
        repository.deleteAll();
    }

    @Test
    public void test() {
        Thread thread = new Thread(() -> {
            tenantResolver.forceTenant(tenant);
            jobInfoService.findHighestPriorityQueuedJobAndSetAsToBeRun();
        });
        thread.start();
        testJobInfoService.findHighestPriorityQueuedJobAndSetAsToBeRun();
    }
}
