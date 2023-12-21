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

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link EurekaInstanceAutoConfiguration}
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

	@BeforeEach
	public void setup() {
		contextRunner = new WebApplicationContextRunner()
			.withPropertyValues("eureka.client.serviceUrl.defaultZone=" + ZONE_URI,
					"vcap.application.uris[0]=" + HOSTNAME, "vcap.application.instance_id=" + INSTANCE_ID,
					"vcap.application.application_id=" + INSTANCE_GUID, "cf.instance.index=" + INSTANCE_INDEX,
					"cf.instance.internal.ip=" + IP, "port=" + PORT)
			.withConfiguration(AutoConfigurations.of(EurekaInstanceAutoConfiguration.class));
	}

	@Test
	public void routeRegistration() {
		contextRunner.withPropertyValues("spring.cloud.services.registrationMethod=" + ROUTE_REGISTRATION_METHOD)
			.run(context -> {
				assertThat(context).hasSingleBean(EurekaInstanceConfigBean.class);

				assertRouteRegistration(context.getBean(EurekaInstanceConfigBean.class));
			});
	}

	@Test
	public void defaultRegistrationIsRouteRegistration() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(EurekaInstanceConfigBean.class);

			assertRouteRegistration(context.getBean(EurekaInstanceConfigBean.class));
		});
	}

	private static void assertRouteRegistration(EurekaInstanceConfigBean config) {
		assertThat(config.getInstanceId()).isEqualTo(HOSTNAME + ":" + INSTANCE_ID);
		assertThat(config.getHostname()).isEqualTo(HOSTNAME);
		assertThat(config.getNonSecurePort()).isEqualTo(80);
		assertThat(config.getSecurePort()).isEqualTo(443);
		assertThat(config.getSecurePortEnabled()).isTrue();

		var metadata = config.getMetadataMap();
		assertThat(metadata.get("cfAppGuid")).isEqualTo(INSTANCE_GUID);
		assertThat(metadata.get("cfInstanceIndex")).isEqualTo(INSTANCE_INDEX);
		assertThat(metadata.get("instanceId")).isEqualTo(INSTANCE_ID);
		assertThat(metadata.get("zone")).isEqualTo(ZONE);
	}

	@Test
	public void directRegistration() {
		contextRunner.withPropertyValues("spring.cloud.services.registrationMethod=" + DIRECT_REGISTRATION_METHOD)
			.run(context -> {
				assertThat(context).hasSingleBean(EurekaInstanceConfigBean.class);

				var config = context.getBean(EurekaInstanceConfigBean.class);
				assertThat(config.isPreferIpAddress()).isTrue();
				assertThat(config.getInstanceId()).isEqualTo(IP + ":" + INSTANCE_ID);
				assertThat(config.getHostname()).isEqualTo(IP);
				assertThat(config.getNonSecurePort()).isEqualTo(PORT);
				assertThat(config.getSecurePortEnabled()).isFalse();
			});
	}

	@Test
	public void shouldNotAcceptEmptyDefaultZoneUri() {
		contextRunner.withPropertyValues("eureka.client.serviceUrl.defaultZone=").run(context -> {
			assertThat(context).hasSingleBean(EurekaInstanceConfigBean.class);

			var eurekaInstanceConfigBean = context.getBean(EurekaInstanceConfigBean.class);
			assertThat(eurekaInstanceConfigBean.getMetadataMap().get("zone")).isEqualTo(UNKNOWN_ZONE);
		});
	}

	@Test
	public void shouldNotAcceptShortDefaultZoneUri() {
		contextRunner.withPropertyValues("eureka.client.serviceUrl.defaultZone=https://funkylocaldomainname/eureka/")
			.run(context -> {
				assertThat(context).hasSingleBean(EurekaInstanceConfigBean.class);

				var eurekaInstanceConfigBean = context.getBean(EurekaInstanceConfigBean.class);
				assertThat(eurekaInstanceConfigBean.getMetadataMap().get("zone")).isEqualTo(UNKNOWN_ZONE);
			});
	}

	@Test
	public void shouldNotAcceptMalformedDefaultZoneUri() {
		contextRunner.withPropertyValues("eureka.client.serviceUrl.defaultZone=:").run(context -> {
			assertThat(context).hasSingleBean(EurekaInstanceConfigBean.class);

			var eurekaInstanceConfigBean = context.getBean(EurekaInstanceConfigBean.class);
			assertThat(eurekaInstanceConfigBean.getMetadataMap().get("zone")).isEqualTo(UNKNOWN_ZONE);
		});
	}

}
