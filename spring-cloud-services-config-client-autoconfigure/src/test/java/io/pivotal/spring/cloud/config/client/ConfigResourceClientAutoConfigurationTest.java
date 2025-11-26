/*
 * Copyright 2017-2024 the original author or authors.
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

package io.pivotal.spring.cloud.config.client;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest(proxyMode = true)
public class ConfigResourceClientAutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(ConfigResourceClientAutoConfiguration.class, ConfigClientAutoConfiguration.class));

	@Test
	void shouldNotCreateConfigResourceClientWhenRestClientIsMissing() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(ConfigResourceClient.class));
	}

	@Test
	void shouldCreateConfigResourceClientWhenRestClientIsPresent() {
		contextRunner.withBean("configClientRestClient", RestClient.class, RestClient::create)
			.run(context -> assertThat(context).hasSingleBean(ConfigResourceClient.class));
	}

}
