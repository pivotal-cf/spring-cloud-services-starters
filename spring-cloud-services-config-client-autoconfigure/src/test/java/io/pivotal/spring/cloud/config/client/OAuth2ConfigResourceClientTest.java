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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Daniel Lavoie
 * @author Dylan Roberts
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ConfigServerTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "spring.profiles.active=plaintext,native", "spring.cloud.config.enabled=true",
				"eureka.client.enabled=false", "spring.config.import=optional:configserver:" })
public class OAuth2ConfigResourceClientTest {

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

	@Before
	public void setUp() {
		configClientProperties = new ConfigClientProperties(new MockEnvironment());
		configClientProperties.setName("app");
		configClientProperties.setProfile(null);
		configClientProperties.setUri(new String[] { "http://localhost:" + port });
		configClient = new OAuth2ConfigResourceClient(new RestTemplate(), configClientProperties);
	}

	@Test
	public void shouldFindSimplePlainFile() {
		Assert.assertEquals(NGINX_CONFIG, read(configClient.getPlainTextResource(null, "master", "nginx.conf")));

		Assert.assertEquals(DEV_NGINX_CONFIG, read(configClient.getPlainTextResource("dev", "master", "nginx.conf")));

		configClientProperties.setProfile("test");
		Assert.assertEquals(TEST_NGINX_CONFIG, read(configClient.getPlainTextResource(null, "master", "nginx.conf")));
	}

	@Test(expected = HttpClientErrorException.class)
	public void missingConfigFileShouldReturnHttpError() {
		configClient.getPlainTextResource(null, "master", "missing-config.xml");
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingApplicationNameShouldCrash() {
		configClientProperties.setName("");
		configClient.getPlainTextResource(null, "master", "nginx.conf");
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingConfigServerUrlShouldCrash() {
		configClientProperties.setUri(new String[] { "" });
		configClient.getPlainTextResource(null, "master", "nginx.conf");
	}

	public String read(Resource resource) {
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
			return buffer.lines().collect(Collectors.joining("\n"));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
