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

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;

/**
 * @author Dylan Roberts
 */
public class EurekaClientOAuth2AutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(EurekaClientOAuth2AutoConfiguration.class));

	@Test
	public void oauth2RequestFactorySupplierIsNotCreated() {
		contextRunner
			.run(context -> assertThat(context).doesNotHaveBean(EurekaClientOAuth2HttpRequestFactorySupplier.class));
	}

	@Test
	public void oauth2RequestFactorySupplierIsCreated() {
		var pairs = oauth2Properties("::id::", "::secret::", "::uri::");

		contextRunner.withPropertyValues(pairs)
			.run(context -> assertThat(context).hasSingleBean(EurekaClientOAuth2HttpRequestFactorySupplier.class));
	}

	@Test
	public void authorizationInterceptorIsConfigured() {
		var pairs = oauth2Properties("::id::", "::secret::", "::uri::");

		contextRunner.withPropertyValues(pairs).run(context -> {
			assertThat(context).hasSingleBean(EurekaClientOAuth2HttpRequestFactorySupplier.class);
			var supplier = context.getBean(EurekaClientOAuth2HttpRequestFactorySupplier.class);

			var clientRegistration = getAuthInterceptorConfiguration(supplier);
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
		var scope = "eureka.client.oauth2.scope=profile,email";
		contextRunner.withPropertyValues(pairs).withPropertyValues(scope).run(context -> {
			assertThat(context).hasSingleBean(EurekaClientOAuth2HttpRequestFactorySupplier.class);
			var supplier = context.getBean(EurekaClientOAuth2HttpRequestFactorySupplier.class);

			var clientRegistration = getAuthInterceptorConfiguration(supplier);
			assertThat(clientRegistration.getClientId()).isEqualTo("::client id::");
			assertThat(clientRegistration.getClientSecret()).isEqualTo("::client secret::");
			assertThat(clientRegistration.getProviderDetails().getTokenUri()).isEqualTo("::token uri::");
			assertThat(clientRegistration.getAuthorizationGrantType()).isEqualTo(CLIENT_CREDENTIALS);
			assertThat(clientRegistration.getScopes()).containsExactlyInAnyOrder("email", "profile");
		});
	}

	private ClientRegistration getAuthInterceptorConfiguration(EurekaClientOAuth2HttpRequestFactorySupplier supplier) {
		var interceptor = (OAuth2AuthorizedClientHttpRequestInterceptor) ReflectionTestUtils.getField(supplier,
				"oAuth2AuthorizedClientHttpRequestInterceptor");
		assertThat(interceptor).isNotNull();

		ClientRegistration clientRegistration = (ClientRegistration) ReflectionTestUtils.getField(interceptor,
				"clientRegistration");
		assertThat(clientRegistration).isNotNull();
		return clientRegistration;
	}

	private String[] oauth2Properties(String clientId, String clientSecret, String tokenUri) {
		return new String[] { String.format("eureka.client.oauth2.client-id=%s", clientId),
				String.format("eureka.client.oauth2.client-secret=%s", clientSecret),
				String.format("eureka.client.oauth2.access-token-uri=%s", tokenUri) };
	}

}