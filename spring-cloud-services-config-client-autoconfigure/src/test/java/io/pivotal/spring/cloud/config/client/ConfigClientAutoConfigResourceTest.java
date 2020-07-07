/*
 * Copyright 2017 the original author or authors.
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

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServiceBootstrapConfiguration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigClientAutoConfigResourceTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withAllowBeanDefinitionOverriding(true)
			.withConfiguration(AutoConfigurations.of(ConfigResourceClientAutoConfiguration.class,
					ConfigClientAutoConfiguration.class, ConfigClientOAuth2BootstrapConfiguration.class,
					ConfigServiceBootstrapConfiguration.class));

	@Test
	public void plainTextConfigClientIsNotCreated() throws Exception {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(ConfigClientProperties.class);
			assertThat(context).doesNotHaveBean(PlainTextConfigClient.class);
		});
	}

	@Test
	public void plainTextConfigClientIsCreated() throws Exception {
		this.contextRunner.withPropertyValues("spring.cloud.config.client.oauth2.client-id=acme",
				"spring.cloud.config.client.oauth2.client-secret=acmesecret",
				"spring.cloud.config.client.oauth2.access-token-uri=acmetokenuri").run(context -> {
					assertThat(context).hasSingleBean(ConfigClientProperties.class);
					assertThat(context).hasSingleBean(OAuth2ConfigResourceClient.class);
					OAuth2ConfigResourceClient plainTextConfigClient = context
							.getBean(OAuth2ConfigResourceClient.class);
					RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(plainTextConfigClient,
							"restTemplate");
					assertThat(restTemplate).isNotNull();
					assertThat(restTemplate.getInterceptors()).hasSize(1);
					assertThat(restTemplate.getInterceptors().get(0))
							.isInstanceOf(OAuth2AuthorizedClientHttpRequestInterceptor.class);
					OAuth2AuthorizedClientHttpRequestInterceptor interceptor = (OAuth2AuthorizedClientHttpRequestInterceptor) restTemplate
							.getInterceptors().get(0);
					ClientRegistration clientRegistration = interceptor.clientRegistration;
					assertThat(clientRegistration.getClientId()).isEqualTo("acme");
					assertThat(clientRegistration.getClientSecret()).isEqualTo("acmesecret");
					assertThat(clientRegistration.getProviderDetails().getTokenUri()).isEqualTo("acmetokenuri");
					assertThat(clientRegistration.getAuthorizationGrantType())
							.isEqualTo(AuthorizationGrantType.CLIENT_CREDENTIALS);
				});
	}

}
