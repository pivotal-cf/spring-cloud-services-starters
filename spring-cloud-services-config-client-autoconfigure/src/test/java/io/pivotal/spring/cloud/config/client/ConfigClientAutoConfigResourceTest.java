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
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;

public class ConfigClientAutoConfigResourceTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withAllowBeanDefinitionOverriding(true)
		.withConfiguration(AutoConfigurations.of(ConfigResourceClientAutoConfiguration.class,
				ConfigClientAutoConfiguration.class));

	@Test
	public void plainTextConfigClientIsNotCreated() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(ConfigClientProperties.class);
			assertThat(context).doesNotHaveBean(PlainTextConfigClient.class);
		});
	}

	@Test
	public void plainTextConfigClientIsCreated() {
		var pairs = oauth2Properties("::id::", "::secret::", "::uri::");

		contextRunner.withPropertyValues(pairs).run(context -> {
			assertThat(context).hasSingleBean(ConfigClientProperties.class);
			assertThat(context).hasSingleBean(PlainTextConfigClient.class);
		});
	}

	@Test
	public void authorizationInterceptorIsConfigured() {
		var pairs = oauth2Properties("::id::", "::secret::", "::uri::");

		contextRunner.withPropertyValues(pairs).run(context -> {
			assertThat(context).hasSingleBean(OAuth2ConfigResourceClient.class);
			var config = context.getBean(OAuth2ConfigResourceClient.class);

			var clientRegistration = getAuthInterceptorConfiguration(config);
			assertThat(clientRegistration.getClientId()).isEqualTo("::id::");
			assertThat(clientRegistration.getClientSecret()).isEqualTo("::secret::");
			assertThat(clientRegistration.getProviderDetails().getTokenUri()).isEqualTo("::uri::");
			assertThat(clientRegistration.getAuthorizationGrantType()).isEqualTo(CLIENT_CREDENTIALS);
			assertThat(clientRegistration.getScopes()).isNull();
		});
	}

	@Test
	public void optionalScopePropertyIsSupported() {
		var pairs = oauth2Properties("::client id::", "::client secret::", "::token uri::");
		var scope = "spring.cloud.config.client.oauth2.scope=profile,email";
		contextRunner.withPropertyValues(pairs).withPropertyValues(scope).run(context -> {
			assertThat(context).hasSingleBean(OAuth2ConfigResourceClient.class);
			var config = context.getBean(OAuth2ConfigResourceClient.class);

			var clientRegistration = getAuthInterceptorConfiguration(config);
			assertThat(clientRegistration.getClientId()).isEqualTo("::client id::");
			assertThat(clientRegistration.getClientSecret()).isEqualTo("::client secret::");
			assertThat(clientRegistration.getProviderDetails().getTokenUri()).isEqualTo("::token uri::");
			assertThat(clientRegistration.getAuthorizationGrantType()).isEqualTo(CLIENT_CREDENTIALS);
			assertThat(clientRegistration.getScopes()).containsExactlyInAnyOrder("email", "profile");
		});
	}

	private ClientRegistration getAuthInterceptorConfiguration(OAuth2ConfigResourceClient config) {
		var restTemplate = (RestTemplate) ReflectionTestUtils.getField(config, "restTemplate");
		assertThat(restTemplate).isNotNull();

		var interceptors = restTemplate.getInterceptors();
		assertThat(interceptors).hasSize(1);
		assertThat(interceptors.get(0)).isInstanceOf(OAuth2AuthorizedClientHttpRequestInterceptor.class);
		var interceptor = (OAuth2AuthorizedClientHttpRequestInterceptor) interceptors.get(0);
		return interceptor.clientRegistration;
	}

	private String[] oauth2Properties(String clientId, String clientSecret, String tokenUri) {
		return new String[] { String.format("spring.cloud.config.client.oauth2.client-id=%s", clientId),
				String.format("spring.cloud.config.client.oauth2.client-secret=%s", clientSecret),
				String.format("spring.cloud.config.client.oauth2.access-token-uri=%s", tokenUri) };
	}

}
