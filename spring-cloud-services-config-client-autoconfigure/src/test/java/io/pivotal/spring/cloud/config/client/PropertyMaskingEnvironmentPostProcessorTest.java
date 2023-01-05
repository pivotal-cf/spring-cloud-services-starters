/*
 * Copyright 2021 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyMaskingEnvironmentPostProcessorTest {

	private static final String VAULT_TEST_SANITIZE_PROPERTY = "MyHiddenVaultData";

	private static final String CREDHUB_TEST_SANITIZE_PROPERTY = "MyHiddenCredhubData";

	private static final String GIT_TEST_NON_SANITIZE_PROPERTY = "ReadableProperty";

	@SpringBootApplication
	public static class TestVaultApplication {

	}

	public static class VaultPropertySourceContextLoader extends SpringBootContextLoader {

		@Override
		protected ConfigurableEnvironment getEnvironment() {
			// Add vault properties that will be masked
			Map<String, Object> fakeVaultProperties = new HashMap<>();
			fakeVaultProperties.put(VAULT_TEST_SANITIZE_PROPERTY, "SecretVaultValue");
			MapPropertySource vaultProperties = new MapPropertySource("configserver:vault:test-data",
					fakeVaultProperties);
			// Add credhub properties that will be masked
			Map<String, Object> fakeCredhubProperties = new HashMap<>();
			fakeCredhubProperties.put(CREDHUB_TEST_SANITIZE_PROPERTY, "SecretCredhubValue");
			MapPropertySource credhubProperties = new MapPropertySource("configserver:credhub-test-data",
					fakeCredhubProperties);

			// Add Git properties that will not be masked (except the my-password which is
			// part of the default sanitize keys)
			Map<String, Object> fakeGitProperties = new HashMap<>();
			fakeGitProperties.put(GIT_TEST_NON_SANITIZE_PROPERTY, "ReadableValue");
			fakeGitProperties.put("my-password", "supersecret");
			MapPropertySource gitProperties = new MapPropertySource("configserver:git:test-data", fakeGitProperties);

			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(vaultProperties);
			environment.getPropertySources().addFirst(credhubProperties);
			environment.getPropertySources().addFirst(gitProperties);
			return environment;
		}

	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@ActiveProfiles({ "integration-test", "native" })
	@ContextConfiguration(classes = TestVaultApplication.class, loader = VaultPropertySourceContextLoader.class)
	public static class TestVaultConfigClientProperties {

		@Autowired
		Environment environment;

		@Test
		public void vaultPropertyIsIncludedInSanitizeEndpoints() {
			String sanitizeEndpointsProp = environment
					.getProperty(PropertyMaskingEnvironmentPostProcessor.SANITIZE_ENV_KEY);

			assertThat(sanitizeEndpointsProp).isNotNull();
			assertThat(sanitizeEndpointsProp).contains(VAULT_TEST_SANITIZE_PROPERTY);
		}

		@Test
		public void credhubPropertyIsIncludedInSanitizeEndpoints() {
			String sanitizeEndpointsProp = environment
					.getProperty(PropertyMaskingEnvironmentPostProcessor.SANITIZE_ENV_KEY);

			assertThat(sanitizeEndpointsProp).isNotNull();
			assertThat(sanitizeEndpointsProp).contains(CREDHUB_TEST_SANITIZE_PROPERTY);
		}

		@Test
		public void gitPropertyIsNotIncludedInSanitizeEndpoints() {
			String sanitizeEndpointsProp = environment
					.getProperty(PropertyMaskingEnvironmentPostProcessor.SANITIZE_ENV_KEY);

			assertThat(sanitizeEndpointsProp).isNotNull();
			assertThat(sanitizeEndpointsProp).doesNotContain(GIT_TEST_NON_SANITIZE_PROPERTY);
		}

	}

}
