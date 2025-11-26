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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.not;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Lavoie
 */
@SpringBootTest(classes = ConfigResourceClientAutoConfiguration.class,
		properties = { "spring.config.import=optional:configserver:", "spring.cloud.config.enabled=true",
				"spring.cloud.config.label=main", "spring.cloud.config.uri=http://localhost:8888",
				"spring.cloud.config.client.oauth2.client-id=id",
				"spring.cloud.config.client.oauth2.client-secret=secret",
				"spring.cloud.config.client.oauth2.access-token-uri=http://localhost:9999/token/uri" })
public class ConfigResourceClientTests {

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

	@Autowired
	private ConfigResourceClient configResourceClient;

	@Test
	public void shouldLoadPlainText() throws IOException {
		configServer.stubFor(
				get("/application/development/dev/path").withHeader("Accept", not(equalTo("application/octet-stream")))
					.willReturn(aResponse().withHeader("Content-Type", "plain/text").withBody("::text::")));

		var resource = this.configResourceClient.getPlainTextResource("development", "dev", "path");

		assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("::text::");
	}

	@Test
	public void shouldLoadBinaryResource() throws IOException {
		configServer
			.stubFor(get("/application/production/prd/path").withHeader("Accept", equalTo("application/octet-stream"))
				.willReturn(aResponse().withHeader("Content-Type", "application/octet-stream").withBody("::binary::")));

		var resource = this.configResourceClient.getBinaryResource("production", "prd", "path");

		assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("::binary::");
	}

	@Test
	public void shouldLoadPlainTextWithDefaultProfileAndLabel() throws IOException {
		configServer.stubFor(get("/application/default/main/path")
			.withHeader("Accept", not(equalTo("application/octet-stream")))
			.willReturn(
					aResponse().withHeader("Content-Type", "application/octet-stream").withBody("::default text::")));

		var resource = this.configResourceClient.getPlainTextResource("path");
		assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("::default text::");
	}

	@Test
	public void shouldLoadBinaryResourceWithDefaultProfileAndLabel() throws IOException {
		configServer.stubFor(get("/application/default/main/path")
			.withHeader("Accept", equalTo("application/octet-stream"))
			.willReturn(
					aResponse().withHeader("Content-Type", "application/octet-stream").withBody("::default binary::")));

		var resource = this.configResourceClient.getBinaryResource("path");

		assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("::default binary::");
	}

}
