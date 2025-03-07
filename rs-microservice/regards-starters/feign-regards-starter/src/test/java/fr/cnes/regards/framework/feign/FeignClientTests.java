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
package fr.cnes.regards.framework.feign;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.metric.autoconfigure.MetricAutoConfiguration;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

import java.util.List;
import java.util.Map;

/**
 * @author Marc Sordi
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("feign")
@EnableAutoConfiguration(exclude = { SecurityAutoConfiguration.class,
                                     ManagementWebSecurityAutoConfiguration.class,
                                     MetricAutoConfiguration.class })
@SpringBootTest(classes = FeignClientTests.Application.class, webEnvironment = WebEnvironment.DEFINED_PORT)
public class FeignClientTests {

    private static final String HELLO_MESSAGE = "Hello world";

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(FeignClientTests.class);

    @Autowired
    private ApplicationContext context;

    @Autowired
    private IHelloClient helloClient;

    @SpringBootApplication(exclude = SecurityAutoConfiguration.class)
    @RestController
    @EnableFeignClients("unkown.package")
    protected static class Application {

        @RequestMapping(method = RequestMethod.GET, value = "/hello")
        @ResourceAccess(role = DefaultRole.PROJECT_ADMIN, description = "None")
        public ResponseEntity<Hello> getHello() {
            Hello hello = new Hello();
            hello.setMessage(HELLO_MESSAGE);
            return ResponseEntity.ok(hello);
        }

        @RequestMapping(method = RequestMethod.GET, value = "/hello503")
        public ResponseEntity<List<Hello>> getHello503() {
            Hello hello = new Hello();
            hello.setMessage("For Tests");
            return new ResponseEntity<>(Lists.newArrayList(hello), HttpStatus.SERVICE_UNAVAILABLE);
        }

        public static void main(String[] args) {
            new SpringApplicationBuilder(Application.class).run(args);
        }
    }

    public static class Hello {

        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String pMessage) {
            message = pMessage;
        }
    }

    /**
     * Test {@link org.springframework.cloud.openfeign.FeignClient} are discovered properly
     */
    @Test
    public void testAnnnotations() {
        Map<String, Object> beans = this.context.getBeansWithAnnotation(FeignClient.class);
        Assert.assertTrue(beans.size() > 0);
        Assert.assertNotNull("Hello client should exist", helloClient);
    }

    @Test
    public void testHelloClient() {
        FeignSecurityManager.asSystem();
        ResponseEntity<Hello> response = helloClient.getHello();
        Hello hello = response.getBody();
        Assert.assertEquals(HELLO_MESSAGE, hello.getMessage());
    }

    @Test
    public void testHello404Client() {
        FeignSecurityManager.asSystem();
        ResponseEntity<Hello> response = helloClient.getHello404();
        Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testHello503Client() {
        FeignSecurityManager.asSystem();
        try {
            helloClient.getHello503();
            Assert.fail("Feign exception should be thrown");
        } catch (HttpServerErrorException e) {
            Assert.assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), e.getRawStatusCode());
        }
    }

}
