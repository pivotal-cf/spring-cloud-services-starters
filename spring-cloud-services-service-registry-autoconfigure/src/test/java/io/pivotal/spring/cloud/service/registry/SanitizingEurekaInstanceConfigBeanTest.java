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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test cases for {@link SanitizingEurekaInstanceConfigBean}
 */
public class SanitizingEurekaInstanceConfigBeanTest {

	private AnnotationConfigApplicationContext ctx;

	@After
	public void tearDown() {
		this.ctx.close();
	}

	@Test
	public void testAppIdentifiersAreDefaultedIfOnlySpringAppNameIsSet() {
		SanitizingEurekaInstanceConfigBean bean = createBeanWithProps("spring.application.name:san");
		assertEquals("san", bean.getAppname());
		assertEquals("san", bean.getVirtualHostName());
		assertEquals("san", bean.getSecureVirtualHostName());
	}

	@Test
	public void testAppIdentifiersAreSanitisedIfOnlySpringAppNameIsSet() {
		SanitizingEurekaInstanceConfigBean bean = createBeanWithProps("spring.application.name:s_an");
		assertEquals("s-an", bean.getAppname());
		assertEquals("s-an", bean.getVirtualHostName());
		assertEquals("s-an", bean.getSecureVirtualHostName());
	}

	@Test
	public void testAppIdentifiersDefaultToEurekaAppName() {
		SanitizingEurekaInstanceConfigBean bean = createBeanWithProps("spring.application.name:s_an",
				"eureka.instance.appname:e_an");
		assertEquals("e_an", bean.getAppname());
		assertEquals("e_an", bean.getVirtualHostName());
		assertEquals("e_an", bean.getSecureVirtualHostName());
	}

	@Test
	public void testAppIdentifiersCanBeSetToTheSameValueAndAreNotSanitized() {
		SanitizingEurekaInstanceConfigBean bean = createBeanWithProps("spring.application.name:s_an",
				"eureka.instance.appname:app_name", "eureka.instance.virtualHostName:app_name",
				"eureka.instance.secureVirtualHostName:app_name");
		assertEquals("app_name", bean.getAppname());
		assertEquals("app_name", bean.getVirtualHostName());
		assertEquals("app_name", bean.getSecureVirtualHostName());
	}

	@Test
	public void testRelaxedPropertyBinding() {
		SanitizingEurekaInstanceConfigBean bean = createBeanWithProps("spring.application.name:s_an",
				"eureka.instance.appname:app_name", "eureka.instance.virtual_host_name:app_name",
				"eureka.instance.secure-virtual-host-name:app_name");
		assertEquals("app_name", bean.getAppname());
		assertEquals("app_name", bean.getVirtualHostName());
		assertEquals("app_name", bean.getSecureVirtualHostName());
	}

	@Test
	public void testExceptionThrownIfVhnDiffersFromAppName() {
		try {
			createBeanWithProps("spring.application.name:san", "eureka.instance.appname:ean",
					"eureka.instance.virtualHostName:vhn", "eureka.instance.secureVirtualHostName:ean");
		}
		catch (BeanCreationException e) {
			Assert.assertThat(e.getMessage(), Matchers.containsString("eureka.instance.virtualHostName"));
			return;
		}
		Assert.fail();
	}

	@Test
	public void testExceptionThrownIfSvhnDiffersFromAppName() {
		try {
			createBeanWithProps("spring.application.name:san", "eureka.instance.appname:ean",
					"eureka.instance.virtualHostName:ean", "eureka.instance.secureVirtualHostName:svhn");
		}
		catch (BeanCreationException e) {
			Assert.assertThat(e.getMessage(), Matchers.containsString("eureka.instance.secureVirtualHostName"));
			return;
		}
		Assert.fail();
	}

	private SanitizingEurekaInstanceConfigBean createBeanWithProps(String... pairs) {
		this.ctx = new AnnotationConfigApplicationContext();

		List<String> pairs1 = new ArrayList<>(Arrays.asList(pairs));
		pairs1.add("sanitizingEurekaInstanceConfigBean.integration.test:true");
		pairs1.add("eureka.client.enabled:false");
		TestPropertyValues.of(pairs1).applyTo(ctx);
		this.ctx.register(Context.class);
		this.ctx.refresh();

		return this.ctx.getBean(SanitizingEurekaInstanceConfigBean.class);
	}

	@Configuration
	@ComponentScan
	@ConditionalOnProperty("sanitizingEurekaInstanceConfigBean.integration.test")
	@EnableConfigurationProperties
	public static class Context {

		@Bean
		public static PropertySourcesPlaceholderConfigurer getPropertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		public VirtualHostNamesBean getVirtualHostNamesBean() {
			return new VirtualHostNamesBean();
		}

		@Bean
		public SanitizingEurekaInstanceConfigBean getSanitizingEurekaInstanceConfigBean() {
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
