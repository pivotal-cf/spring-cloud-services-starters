/*
 * Copyright 2019 the original author or authors.
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
package io.pivotal.spring.cloud.service.registry;

import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for
 * {@link EurekaInstanceAutoConfiguration}
 *
 * @author Chris Schaefer
 * @author Will Tran
 * @author Dylan Roberts
 */
public class EurekaInstanceAutoConfigurationTest {
	private static final String ROUTE_REGISTRATION_METHOD = "route";
	private static final String DIRECT_REGISTRATION_METHOD = "direct";
	private static final String INSTANCE_GUID = UUID.randomUUID().toString();
	private static final String INSTANCE_INDEX = "12";
	private static final String HOSTNAME = "www.route.com";
	private static final String IP = "1.2.3.4";
	private static final int PORT = 54321;
	private static final String INSTANCE_ID = UUID.randomUUID().toString();
	private static final String ZONE_URI = "https://eureka-123.west.my-cf.com/eureka/";
	private static final String ZONE = "west.my-cf.com";
	private static final String UNKNOWN_ZONE = "unknown";

	private WebApplicationContextRunner contextRunner;

	@Before
	public void setUp() {
		contextRunner = new WebApplicationContextRunner()
				.withPropertyValues(
						"eureka.client.serviceUrl.defaultZone=" + ZONE_URI,
						"vcap.application.uris[0]=" + HOSTNAME,
						"vcap.application.instance_id=" + INSTANCE_ID,
						"vcap.application.application_id=" + INSTANCE_GUID,
						"cf.instance.index=" + INSTANCE_INDEX,
						"cf.instance.internal.ip=" + IP,
						"port=" + PORT
				)
				.withConfiguration(AutoConfigurations.of(EurekaInstanceAutoConfiguration.class));
	}

	@Test
	public void testRouteRegistration() {
		contextRunner = contextRunner.withPropertyValues("spring.cloud.services.registrationMethod=" + ROUTE_REGISTRATION_METHOD);
		testDefaultRegistration();
	}

	@Test
	public void testDefaultRegistration() {
		contextRunner
				.run(context -> {
					assertThat(context).hasSingleBean(EurekaInstanceConfigBean.class);
					EurekaInstanceConfigBean eurekaInstanceConfigBean = context.getBean(EurekaInstanceConfigBean.class);
					assertEquals(HOSTNAME + ":" + INSTANCE_ID, eurekaInstanceConfigBean.getInstanceId());
					assertEquals(HOSTNAME, eurekaInstanceConfigBean.getHostname());
					assertEquals(80, eurekaInstanceConfigBean.getNonSecurePort());
					assertEquals(443, eurekaInstanceConfigBean.getSecurePort());
					assertTrue(eurekaInstanceConfigBean.getSecurePortEnabled());

					Map<String, String> metadata = eurekaInstanceConfigBean.getMetadataMap();
					assertEquals(INSTANCE_GUID, metadata.get("cfAppGuid"));
					assertEquals(INSTANCE_INDEX, metadata.get("cfInstanceIndex"));
					assertEquals(INSTANCE_ID, metadata.get("instanceId"));
					assertEquals(ZONE, metadata.get("zone"));
				});
	}

	@Test
	public void testDirectRegistration() {
		contextRunner
				.withPropertyValues("spring.cloud.services.registrationMethod=" + DIRECT_REGISTRATION_METHOD)
				.run(context -> {
					assertThat(context).hasSingleBean(EurekaInstanceConfigBean.class);
					EurekaInstanceConfigBean eurekaInstanceConfigBean = context.getBean(EurekaInstanceConfigBean.class);
					assertTrue(eurekaInstanceConfigBean.isPreferIpAddress());
					assertEquals(IP + ":" + INSTANCE_ID, eurekaInstanceConfigBean.getInstanceId());
					assertEquals(IP, eurekaInstanceConfigBean.getHostname());
					assertEquals(PORT, eurekaInstanceConfigBean.getNonSecurePort());
					assertFalse(eurekaInstanceConfigBean.getSecurePortEnabled());
				});
	}

	@Test
	public void testEmptyDefaultZoneUri() {
		contextRunner
				.withPropertyValues("eureka.client.serviceUrl.defaultZone=")
				.run(context -> {
					assertThat(context).hasSingleBean(EurekaInstanceConfigBean.class);
					EurekaInstanceConfigBean eurekaInstanceConfigBean = context.getBean(EurekaInstanceConfigBean.class);
					assertEquals(UNKNOWN_ZONE, eurekaInstanceConfigBean.getMetadataMap().get("zone"));
				});
	}

	@Test
	public void testShortDefaultZoneUri() {
		contextRunner
				.withPropertyValues("eureka.client.serviceUrl.defaultZone=https://funkylocaldomainname/eureka/")
				.run(context -> {
					assertThat(context).hasSingleBean(EurekaInstanceConfigBean.class);
					EurekaInstanceConfigBean eurekaInstanceConfigBean = context.getBean(EurekaInstanceConfigBean.class);
					assertEquals(UNKNOWN_ZONE, eurekaInstanceConfigBean.getMetadataMap().get("zone"));
				});
	}

	@Test
	public void testMalformedDefaultZoneUri() {
		contextRunner
				.withPropertyValues("eureka.client.serviceUrl.defaultZone=:")
				.run(context -> {
					assertThat(context).hasSingleBean(EurekaInstanceConfigBean.class);
					EurekaInstanceConfigBean eurekaInstanceConfigBean = context.getBean(EurekaInstanceConfigBean.class);
					assertEquals(UNKNOWN_ZONE, eurekaInstanceConfigBean.getMetadataMap().get("zone"));
				});
	}
}
