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

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(classes = ConfigClientOAuth2InterceptorTest.TestApplication.class,
		properties = { "spring.config.import=optional:configserver:", "spring.cloud.config.enabled=true",
				"spring.cloud.config.label=main", "spring.cloud.config.uri=http://localhost:8888",
				"spring.cloud.config.client.oauth2.client-id=id",
				"spring.cloud.config.client.oauth2.client-secret=secret",
				"spring.cloud.config.client.oauth2.scope=profile,email",
				"spring.cloud.config.client.oauth2.access-token-uri=http://localhost:9999/token/uri" })
public class ConfigClientOAuth2InterceptorTest {

	private static WireMockServer uaaServer;

	private static WireMockServer configServer;

	@BeforeAll
	static void setup() {
		uaaServer = new WireMockServer(wireMockConfig().port(9999));
		uaaServer.start();
		uaaServer.stubFor(post("/token/uri")
			.willReturn(aResponse().withHeader("Content-Type", "application/json;charset=UTF-8").withBody("""
					{
					  "access_token" : "access-token",
					  "token_type" : "bearer"
					}""")));

		configServer = new WireMockServer(wireMockConfig().port(8888));
		configServer.start();
		configServer.stubFor(get(urlMatching("/application/.*"))
			.willReturn(aResponse().withHeader("Content-Type", "application/json;charset=UTF-8").withBody("""
					{
					  "name" : "remote-env"
					}""")));
	}

	@AfterAll
	static void cleanup() {
		configServer.stop();
		uaaServer.stop();
	}

	@Test
	void configurationIsLoadedUsingAccessToken() {
		configServer.verify(getRequestedFor(urlEqualTo("/application/default/main")).withHeader("Authorization",
				equalTo("Bearer access-token")));
		configServer.verify(getRequestedFor(urlEqualTo("/application/native/main")).withHeader("Authorization",
				equalTo("Bearer access-token")));
	}

	@Test
	void accessTokenIsRequestedUsingClientOAuthProperties() {
		String base64Credentials = Base64.getEncoder().encodeToString(("id:secret").getBytes(StandardCharsets.UTF_8));

		uaaServer.verify(postRequestedFor(urlEqualTo("/token/uri"))
			.withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
			.withHeader("Authorization", equalTo("Basic " + base64Credentials))
			.withRequestBody(containing("grant_type=client_credentials")));
	}

	@Test
	void optionalScopePropertyShouldBeIncludedInTokenRequest() {
		String base64Credentials = Base64.getEncoder().encodeToString(("id:secret").getBytes(StandardCharsets.UTF_8));

		uaaServer.verify(postRequestedFor(urlEqualTo("/token/uri"))
			.withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
			.withHeader("Authorization", equalTo("Basic " + base64Credentials))
			.withRequestBody(containing("grant_type=client_credentials"))
			.withRequestBody(containing("scope=profile+email")));
	}

	@SpringBootApplication
	static class TestApplication {

	}

}
