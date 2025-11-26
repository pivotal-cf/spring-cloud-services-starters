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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static io.pivotal.spring.cloud.config.client.DefaultConfigResourceClientTest.ConfigServerTestApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Daniel Lavoie
 * @author Dylan Roberts
 */
@SpringBootTest(classes = ConfigServerTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "spring.cloud.config.enabled=true", "spring.config.import=optional:configserver:" })
public class DefaultConfigResourceClientTest {

	// @formatter:off
	private static final String NGINX_CONFIG = """
			server {
			    listen              80;
			    server_name         example.com;
			}""";

	private static final String DEV_NGINX_CONFIG = """
			server {
			    listen              80;
			    server_name         dev.example.com;
			}""";

	private static final String TEST_NGINX_CONFIG = """
			server {
			    listen              80;
			    server_name         test.example.com;
			}""";
	// @formatter:on

	@LocalServerPort
	private int port;

	private ConfigClientProperties configClientProperties;

	private ConfigResourceClient configClient;

	@BeforeEach
	public void setup() {
		configClientProperties = new ConfigClientProperties(new MockEnvironment());
		configClientProperties.setName("app");
		configClientProperties.setProfile("default");
		configClientProperties.setLabel("main");
		configClientProperties.setUri(new String[] { "http://localhost:" + port });
		configClient = new DefaultConfigResourceClient(RestClient.create(), configClientProperties);
	}

	@Test
	public void shouldFindResourcesUsingDefaultProfile() {
		assertThat(read(configClient.getPlainTextResource(null, "main", "nginx.conf"))).isEqualTo(NGINX_CONFIG);
		assertThat(read(configClient.getBinaryResource(null, "main", "nginx.conf"))).isEqualTo(NGINX_CONFIG);
	}

	@Test
	public void shouldFindResourcesUsingDefaultLabel() {
		assertThat(read(configClient.getPlainTextResource("dev", null, "nginx.conf"))).isEqualTo(DEV_NGINX_CONFIG);
		assertThat(read(configClient.getBinaryResource("dev", null, "nginx.conf"))).isEqualTo(DEV_NGINX_CONFIG);
	}

	@Test
	public void shouldFindResourcesUsingDefaultProfileAndLabel() {
		assertThat(read(configClient.getPlainTextResource("nginx.conf"))).isEqualTo(NGINX_CONFIG);
		assertThat(read(configClient.getBinaryResource("nginx.conf"))).isEqualTo(NGINX_CONFIG);
	}

	@Test
	public void shouldFindResourceWithGivenProfileAndLabel() {
		assertThat(read(configClient.getPlainTextResource("test", "main", "nginx.conf"))).isEqualTo(TEST_NGINX_CONFIG);
		assertThat(read(configClient.getBinaryResource("test", "main", "nginx.conf"))).isEqualTo(TEST_NGINX_CONFIG);
	}

	@Test
	public void missingResourceShouldReturnHttpError() {
		assertThatThrownBy(() -> configClient.getPlainTextResource(null, "main", "missing-config.xml"))
			.isInstanceOf(HttpClientErrorException.class);

		assertThatThrownBy(() -> configClient.getBinaryResource(null, "main", "missing-config.bin"))
			.isInstanceOf(HttpClientErrorException.class);
	}

	@Test
	public void missingApplicationNameShouldCrash() {
		configClientProperties.setName("");

		assertThatThrownBy(() -> configClient.getPlainTextResource(null, "main", "nginx.conf"))
			.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> configClient.getBinaryResource(null, "main", "nginx.conf"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void missingConfigServerUrlShouldCrash() {
		configClientProperties.setUri(new String[] { "" });

		assertThatThrownBy(() -> configClient.getPlainTextResource(null, "main", "nginx.conf"))
			.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> configClient.getBinaryResource(null, "main", "nginx.conf"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private String read(Resource resource) {
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
			return buffer.lines().collect(Collectors.joining("\n"));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@EnableConfigServer
	@SpringBootApplication
	static class ConfigServerTestApplication {

		public static void main(String[] args) {
			SpringApplication.run(ConfigServerTestApplication.class);
		}

	}

}
