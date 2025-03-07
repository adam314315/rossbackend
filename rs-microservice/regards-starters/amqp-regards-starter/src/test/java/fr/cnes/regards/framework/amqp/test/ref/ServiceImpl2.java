/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.regards.framework.amqp.test.ref;

import org.junit.Assert;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gary Russell
 */
public class ServiceImpl2 implements Service {

    @Autowired
    private RabbitTemplate template;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(boolean crash) throws Exception {
        Object o = template.receiveAndConvert(Config.QUEUE_NAME);
        Assert.assertNotNull(o);
        if (crash) {
            throw new FooEx("crash");
        }
    }

    private class FooEx extends Exception {

        private FooEx(String message) {
            super(message);
        }

    }
}
