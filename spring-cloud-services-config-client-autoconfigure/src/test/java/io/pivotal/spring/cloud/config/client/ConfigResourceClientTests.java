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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Lavoie
 */
@Import(ConfigResourceClientAutoConfiguration.class)
@SpringBootTest(classes = ConfigClientProperties.class,
		properties = { "spring.cloud.config.label=main", "spring.cloud.config.uri=http://server.local",
				"spring.cloud.config.client.oauth2.client-id=id",
				"spring.cloud.config.client.oauth2.client-secret=secret",
				"spring.cloud.config.client.oauth2.access-token-uri=http://uaa.local/token/uri" })
@WireMockTest(proxyMode = true)
public class ConfigResourceClientTests {

	@Autowired
	private ConfigResourceClient configResourceClient;

	@BeforeEach
	void setup() {
		stubFor(post("/token/uri").withHost(equalTo("uaa.local"))
			.willReturn(aResponse().withHeader("Content-Type", "application/json;charset=UTF-8").withBody("""
					{
					  "access_token" : "access-token",
					  "token_type" : "bearer"
					}""")));
	}

	@Test
	public void shouldLoadPlainText() throws IOException {
		stubFor(get("/application/development/dev/path").withHost(equalTo("server.local"))
			.withHeader("Accept", not(equalTo("application/octet-stream")))
			.willReturn(aResponse().withHeader("Content-Type", "plain/text").withBody("::text::")));

		var resource = configResourceClient.getPlainTextResource("development", "dev", "path");

		assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("::text::");
	}

	@Test
	public void shouldLoadBinaryResource() throws IOException {
		stubFor(get("/application/production/prd/path").withHost(equalTo("server.local"))
			.withHeader("Accept", equalTo("application/octet-stream"))
			.willReturn(aResponse().withHeader("Content-Type", "application/octet-stream").withBody("::binary::")));

		var resource = configResourceClient.getBinaryResource("production", "prd", "path");

		assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("::binary::");
	}

	@Test
	public void shouldLoadPlainTextWithDefaultProfileAndLabel() throws IOException {
		stubFor(get("/application/default/main/path").withHost(equalTo("server.local"))
			.withHeader("Accept", not(equalTo("application/octet-stream")))
			.willReturn(
					aResponse().withHeader("Content-Type", "application/octet-stream").withBody("::default text::")));

		var resource = configResourceClient.getPlainTextResource("path");
		assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("::default text::");
	}

	@Test
	public void shouldLoadBinaryResourceWithDefaultProfileAndLabel() throws IOException {
		stubFor(get("/application/default/main/path").withHost(equalTo("server.local"))
			.withHeader("Accept", equalTo("application/octet-stream"))
			.willReturn(
					aResponse().withHeader("Content-Type", "application/octet-stream").withBody("::default binary::")));

		var resource = configResourceClient.getBinaryResource("path");

		assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("::default binary::");
	}

}
