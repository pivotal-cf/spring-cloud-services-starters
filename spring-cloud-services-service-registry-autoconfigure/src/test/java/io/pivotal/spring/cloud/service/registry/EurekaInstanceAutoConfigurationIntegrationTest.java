/*
 * Copyright 2019-2024 the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roy Clarkson
 * @author Will Tran
 */
public class EurekaInstanceAutoConfigurationIntegrationTest {

	@Nested
	@SpringBootTest(classes = TestApplication.class,
			properties = { "vcap.application.uris[0]=www.route.local", "port=54321", "cf.instance.internal.ip=1.2.3.4",
					"vcap.application.instance_id=instance-id", "spring.application.name=app-name_",
					"spring.cloud.services.registrationMethod=direct",
					"eureka.client.serviceUrl.defaultZone=https://eureka-123.east.my-cf.com/eureka" })
	class DirectRegistrationTest {

		@Autowired
		private EurekaInstanceConfigBean config;

		@Value("${spring.cloud.loadbalancer.configurations:}")
		String loadBalancerConfigurations;

		@Test
		public void eurekaConfigBean() {
			assertThat(this.config.getInstanceId()).isEqualTo("1.2.3.4:instance-id");
			assertThat(this.config.getAppname()).isEqualTo("app-name-");
			assertThat(this.config.getVirtualHostName()).isEqualTo("app-name-");
			assertThat(this.config.getSecureVirtualHostName()).isEqualTo("app-name-");
			assertThat(this.config.getHostname()).isEqualTo("1.2.3.4");
			assertThat(this.config.getNonSecurePort()).isEqualTo(54321);
			assertThat(this.config.getMetadataMap().get("zone")).isEqualTo("east.my-cf.com");
			assertThat(this.config.getSecurePortEnabled()).isFalse();
		}

		@Test
		public void loadBalancerConfigurations() {
			assertThat(this.loadBalancerConfigurations).isEmpty();
		}

	}

	@Nested
	@SpringBootTest(classes = TestApplication.class,
			properties = { "vcap.application.uris[0]=www.route.local", "cf.instance.ip=1.2.3.4",
					"vcap.application.instance_id=instance-id", "spring.application.name=app-name_",
					"spring.cloud.services.registrationMethod=route",
					"eureka.client.serviceUrl.defaultZone=https://eureka-123.west.my-cf.com/eureka/" })
	class RouteRegistrationTest {

		@Autowired
		private EurekaInstanceConfigBean config;

		@Value("${spring.cloud.loadbalancer.configurations:}")
		String loadBalancerConfigurations;

		@Test
		public void eurekaConfigBean() {
			assertThat(this.config.getInstanceId()).isEqualTo("www.route.local:instance-id");
			assertThat(this.config.getAppname()).isEqualTo("app-name-");
			assertThat(this.config.getVirtualHostName()).isEqualTo("app-name-");
			assertThat(this.config.getSecureVirtualHostName()).isEqualTo("app-name-");
			assertThat(this.config.getHostname()).isEqualTo("www.route.local");
			assertThat(this.config.getNonSecurePort()).isEqualTo(80);
			assertThat(this.config.getSecurePort()).isEqualTo(443);
			assertThat(this.config.getMetadataMap().get("zone")).isEqualTo("west.my-cf.com");
			assertThat(this.config.getSecurePortEnabled()).isTrue();
		}

		@Test
		public void loadBalancerConfigurations() {
			assertThat(this.loadBalancerConfigurations).isEmpty();
		}

	}

	@Nested
	@SpringBootTest(classes = TestApplication.class,
			properties = { "vcap.application.uris[0]=www.route.local",
					"scs.starters.eureka.client.zone.configuration.enabled=true",
					"eureka.client.serviceUrl.defaultZone=https://eureka-123.west.my-cf.com/eureka/" })
	class LoadBalancerEnabledRegistrationTest {

		@Autowired
		private EurekaInstanceConfigBean config;

		@Value("${spring.cloud.loadbalancer.configurations:}")
		String loadBalancerConfigurations;

		@Test
		public void eurekaConfigBean() {
			assertThat(this.config.getMetadataMap().get("zone")).isEqualTo("eureka-123.west.my-cf.com");
		}

		@Test
		public void loadBalancerConfigurations() {
			assertThat(this.loadBalancerConfigurations).isEqualTo("zone-preference");
		}

	}

}
