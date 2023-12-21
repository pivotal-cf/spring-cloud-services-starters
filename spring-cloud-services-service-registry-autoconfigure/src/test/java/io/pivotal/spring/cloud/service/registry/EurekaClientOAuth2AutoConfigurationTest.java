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

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dylan Roberts
 */
@WireMockTest(proxyMode = true)
public class EurekaClientOAuth2AutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(EurekaClientOAuth2AutoConfiguration.class));

	@Test
	void configurationIsNotEnabledWhenOAuth2PropertiesAreMissing() {
		contextRunner
			.run(context -> assertThat(context).doesNotHaveBean(EurekaClientOAuth2HttpRequestFactorySupplier.class));
	}

	@Test
	void configurationIsEnabledWhenOAuth2PropertiesArePresent() {
		var pairs = applicationProperties("::id::", "::secret::");

		contextRunner.withPropertyValues(pairs)
			.run(context -> assertThat(context).hasSingleBean(EurekaClientOAuth2HttpRequestFactorySupplier.class));
	}

	@Test
	void authInterceptorIsConfiguredWhenOAuth2PropertiesArePresent() {
		var pairs = applicationProperties("id", "secret");
		String base64Credentials = Base64.getEncoder().encodeToString(("id:secret").getBytes());

		stubTokenEndpoint();

		contextRunner.withPropertyValues(pairs).run(context -> {
			assertThat(context).hasSingleBean(EurekaClientOAuth2HttpRequestFactorySupplier.class);

			callAnyEndpoint(context);

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

		stubTokenEndpoint();

		contextRunner.withPropertyValues(pairs).run(context -> {
			assertThat(context).hasSingleBean(EurekaClientOAuth2HttpRequestFactorySupplier.class);

			callAnyEndpoint(context);

			verify(postRequestedFor(urlEqualTo("/token/uri"))
				.withHeader("Content-Type", equalTo("application/x-www-form-urlencoded;charset=UTF-8"))
				.withHeader("Authorization", equalTo("Basic " + base64Credentials))
				.withRequestBody(containing("grant_type=client_credentials"))
				.withRequestBody(containing("scope=profile+email")));
		});
	}

	private void stubTokenEndpoint() {
		stubFor(post("/token/uri").withHost(equalTo("uaa.local"))
			.willReturn(aResponse().withHeader("Content-Type", "application/json;charset=UTF-8").withBody("""
					{
					  "access_token" : "access-token",
					  "token_type" : "bearer"
					}""")));
	}

	private void callAnyEndpoint(BeanFactory factory) {
		var supplier = factory.getBean(EurekaClientOAuth2HttpRequestFactorySupplier.class);

		try {
			supplier.get(null, null)
				.createRequest(URI.create("http://server.local/ping"), HttpMethod.GET)
				.execute()
				.close();
		}
		catch (IOException e) {
			// Ignore exceptions in the GET call.
		}
	}

	private String[] applicationProperties(String clientId, String clientSecret) {
		return applicationProperties(clientId, clientSecret, "");
	}

	private String[] applicationProperties(String clientId, String clientSecret, String scope) {
		return new String[] { "eureka.client.oauth2.access-token-uri=http://uaa.local/token/uri",
				String.format("eureka.client.oauth2.client-id=%s", clientId),
				String.format("eureka.client.oauth2.client-secret=%s", clientSecret),
				String.format("eureka.client.oauth2.scope=%s", scope), };
	}

}