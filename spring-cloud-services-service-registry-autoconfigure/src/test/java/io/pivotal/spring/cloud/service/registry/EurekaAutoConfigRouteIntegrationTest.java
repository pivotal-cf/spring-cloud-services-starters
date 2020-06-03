/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.pivotal.spring.cloud.service.registry;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Roy Clarkson
 * @author Will Tran
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"vcap.application.uris[0]=www.route.local",
				"cf.instance.ip=1.2.3.4",
				"cf.instance.port=54321",
				"eureka.client.serviceUrl.defaultZone=https://eureka-123.west.my-cf.com/eureka/",
				"vcap.application.instance_id=instance-id",
				"spring.application.name=app-name_",
				"spring.cloud.services.registrationMethod=route",
				"eureka.client.enabled=true"
		})
public class EurekaAutoConfigRouteIntegrationTest {

	@Autowired
	private EurekaInstanceConfigBean config;

	@Test
	public void eurekaConfigBean() {
		assertEquals("www.route.local:instance-id", config.getInstanceId());
		assertEquals("app-name-", config.getAppname());
		assertEquals("app-name-", config.getVirtualHostName());
		assertEquals("app-name-", config.getSecureVirtualHostName());
		assertEquals("www.route.local", config.getHostname());
		assertEquals(80, config.getNonSecurePort());
		assertEquals(443, config.getSecurePort());
		assertTrue(config.getSecurePortEnabled());
		assertEquals("west.my-cf.com", config.getMetadataMap().get("zone"));
	}

}
