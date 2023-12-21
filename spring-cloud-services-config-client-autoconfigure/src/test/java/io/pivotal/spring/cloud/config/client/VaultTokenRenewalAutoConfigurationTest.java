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

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThan;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.pivotal.spring.cloud.config.client.VaultTokenRenewalAutoConfiguration.VaultTokenRefresher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Roy Clarkson
 * @author Dylan Roberts
 */
@WireMockTest(proxyMode = true)
public class VaultTokenRenewalAutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(ConfigClientAutoConfiguration.class, VaultTokenRenewalAutoConfiguration.class));

	@Test
	void configurationIsNotEnabledWhenOAuth2PropertiesAreMissing() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(VaultTokenRefresher.class));
	}

	@Test
	void configurationIsEnabledWhenOAuth2PropertiesArePresent() {
		var pairs = applicationProperties("::id::", "::secret::");

		contextRunner.withPropertyValues(pairs)
			.run(context -> assertThat(context).hasSingleBean(VaultTokenRefresher.class));
	}

	@Test
	void authInterceptorIsConfiguredWhenOAuth2PropertiesArePresent() {
		var pairs = applicationProperties("id", "secret");
		String base64Credentials = Base64.getEncoder().encodeToString(("id:secret").getBytes());

		stubTokenEndpoints();

		contextRunner.withPropertyValues(pairs).run(context -> {
			assertThat(context).hasSingleBean(VaultTokenRefresher.class);

			tryRefreshingToken(context);

			verify(postRequestedFor(urlEqualTo("/token/uri"))
				.withHeader("Content-Type", equalTo("application/x-www-form-urlencoded;charset=UTF-8"))
				.withHeader("Authorization", equalTo("Basic " + base64Credentials))
				.withRequestBody(containing("grant_type=client_credentials")));
		});
	}

	@Test
	void optionalScopePropertyShouldBeIncludedInTokenRequest() {
		var pairs = applicationProperties("id", "secret", "profile,email");
		String base64Credentials = Base64.getEncoder().encodeToString(("id:secret").getBytes());

		stubTokenEndpoints();

		contextRunner.withPropertyValues(pairs).run(context -> {
			assertThat(context).hasSingleBean(VaultTokenRefresher.class);

			tryRefreshingToken(context);

			verify(postRequestedFor(urlEqualTo("/token/uri"))
				.withHeader("Content-Type", equalTo("application/x-www-form-urlencoded;charset=UTF-8"))
				.withHeader("Authorization", equalTo("Basic " + base64Credentials))
				.withRequestBody(containing("grant_type=client_credentials"))
				.withRequestBody(containing("scope=profile+email")));
		});
	}

	@Test
	void schedulesVaultTokenRefresh() {
		var pairs = applicationProperties("id", "secret", "profile,email");

		stubTokenEndpoints();

		contextRunner.withPropertyValues(pairs).run(context -> {
			assertThat(context).hasSingleBean(VaultTokenRefresher.class);

			await().atMost(3L, TimeUnit.SECONDS).untilAsserted(() -> {
				verify(moreThan(2),
						postRequestedFor(urlEqualTo("/vault/v1/auth/token/renew-self"))
							.withHeader("Content-Type", equalTo("application/json"))
							.withHeader("Authorization", equalTo("Bearer access-token"))
							.withHeader("X-Vault-Token", equalTo("vault-token"))
							.withRequestBody(containing("{\"increment\":300}")));
			});
		});
	}

	private void stubTokenEndpoints() {
		stubFor(post("/token/uri").withHost(equalTo("uaa.local"))
			.willReturn(aResponse().withHeader("Content-Type", "application/json;charset=UTF-8").withBody("""
					{
					  "access_token" : "access-token",
					  "token_type" : "bearer"
					}""")));

		stubFor(post("/vault/v1/auth/token/renew-self").withHost(equalTo("server.local"))
			.willReturn(aResponse().withHeader("Content-Type", "plain/text").withBody("new-token")));
	}

	private void tryRefreshingToken(BeanFactory factory) {
		factory.getBean(VaultTokenRefresher.class).refreshVaultToken();
	}

	private String[] applicationProperties(String clientId, String clientSecret) {
		return applicationProperties(clientId, clientSecret, "");
	}

	private String[] applicationProperties(String clientId, String clientSecret, String scope) {
		return new String[] { "vault.token.renew.rate=1000", "spring.cloud.config.token=vault-token",
				"spring.cloud.config.uri=http://server.local",
				"spring.cloud.config.client.oauth2.access-token-uri=http://uaa.local/token/uri",
				String.format("spring.cloud.config.client.oauth2.client-id=%s", clientId),
				String.format("spring.cloud.config.client.oauth2.client-secret=%s", clientSecret),
				String.format("spring.cloud.config.client.oauth2.scope=%s", scope), };
	}

}
