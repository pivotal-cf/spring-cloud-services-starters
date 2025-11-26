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

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test cases for {@link SanitizingEurekaInstanceConfigBean}
 */
public class SanitizingEurekaInstanceConfigBeanTest {

	@Test
	public void appIdentifiersAreDefaultedIfOnlySpringAppNameIsSet() {
		var bean = createBeanWithProps("spring.application.name:san");

		assertThat(bean.getAppname()).isEqualTo("san");
		assertThat(bean.getVirtualHostName()).isEqualTo("san");
		assertThat(bean.getSecureVirtualHostName()).isEqualTo("san");
	}

	@Test
	public void appIdentifiersAreSanitisedIfOnlySpringAppNameIsSet() {
		var bean = createBeanWithProps("spring.application.name:s_a.n");

		assertThat(bean.getAppname()).isEqualTo("s-a.n");
		assertThat(bean.getVirtualHostName()).isEqualTo("s-a.n");
		assertThat(bean.getSecureVirtualHostName()).isEqualTo("s-a.n");
	}

	@Test
	public void appIdentifiersDefaultToEurekaAppName() {
		var config = createBeanWithProps("spring.application.name:s_an", "eureka.instance.appname:e_an");

		assertThat(config.getAppname()).isEqualTo("e_an");
		assertThat(config.getVirtualHostName()).isEqualTo("e_an");
		assertThat(config.getSecureVirtualHostName()).isEqualTo("e_an");
	}

	@Test
	public void appIdentifiersCanBeSetToTheSameValueAndAreNotSanitized() {
		var config = createBeanWithProps("spring.application.name:s_an", "eureka.instance.appname:app_name",
				"eureka.instance.virtualHostName:app_name", "eureka.instance.secureVirtualHostName:app_name");

		assertThat(config.getAppname()).isEqualTo("app_name");
		assertThat(config.getVirtualHostName()).isEqualTo("app_name");
		assertThat(config.getSecureVirtualHostName()).isEqualTo("app_name");
	}

	@Test
	public void relaxedPropertyBinding() {
		var config = createBeanWithProps("spring.application.name:s_an", "eureka.instance.appname:app_name",
				"eureka.instance.virtual_host_name:app_name", "eureka.instance.secure-virtual-host-name:app_name");

		assertThat(config.getAppname()).isEqualTo("app_name");
		assertThat(config.getVirtualHostName()).isEqualTo("app_name");
		assertThat(config.getSecureVirtualHostName()).isEqualTo("app_name");
	}

	@Test
	public void exceptionThrownIfVhnDiffersFromAppName() {
		assertThatThrownBy(() -> createBeanWithProps("spring.application.name:san", "eureka.instance.appname:ean",
				"eureka.instance.virtualHostName:vhn", "eureka.instance.secureVirtualHostName:ean"))
			.isInstanceOf(BeanCreationException.class)
			.hasMessageContaining("eureka.instance.virtualHostName");

	}

	@Test
	public void exceptionThrownIfSvhnDiffersFromAppName() {
		assertThatThrownBy(() -> createBeanWithProps("spring.application.name:san", "eureka.instance.appname:ean",
				"eureka.instance.virtualHostName:ean", "eureka.instance.secureVirtualHostName:svhn"))
			.isInstanceOf(BeanCreationException.class)
			.hasMessageContaining("eureka.instance.secureVirtualHostName");
	}

	private SanitizingEurekaInstanceConfigBean createBeanWithProps(String... pairs) {
		var values = new ArrayList<>(Arrays.asList(pairs));
		values.add("sanitizingEurekaInstanceConfigBean.integration.test:true");
		values.add("eureka.client.enabled:false");

		try (var context = new AnnotationConfigApplicationContext()) {
			TestPropertyValues.of(values).applyTo(context);
			context.register(Context.class);
			context.refresh();

			return context.getBean(SanitizingEurekaInstanceConfigBean.class);
		}
	}

	@Configuration
	@ComponentScan
	@ConditionalOnProperty("sanitizingEurekaInstanceConfigBean.integration.test")
	@EnableConfigurationProperties
	public static class Context {

		@Bean
		static PropertySourcesPlaceholderConfigurer getPropertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		VirtualHostNamesBean getVirtualHostNamesBean() {
			return new VirtualHostNamesBean();
		}

		@Bean
		SanitizingEurekaInstanceConfigBean getSanitizingEurekaInstanceConfigBean() {
			return new SanitizingEurekaInstanceConfigBean(getInetUtils());
		}

		private InetUtils getInetUtils() {

			InetUtils.HostInfo hostInfo = new InetUtils(new InetUtilsProperties()).findFirstNonLoopbackHostInfo();

			InetUtils inetUtils = mock(InetUtils.class);
			when(inetUtils.findFirstNonLoopbackHostInfo()).thenReturn(hostInfo);

			return inetUtils;
		}

	}

}
