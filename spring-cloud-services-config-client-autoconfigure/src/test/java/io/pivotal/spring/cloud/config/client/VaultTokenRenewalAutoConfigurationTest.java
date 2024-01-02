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

import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.pivotal.spring.cloud.config.client.VaultTokenRenewalAutoConfiguration.VaultTokenRefresher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Roy Clarkson
 * @author Dylan Roberts
 */
@WireMockTest(proxyMode = true)
public class VaultTokenRenewalAutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(
				AutoConfigurations.of(ConfigClientAutoConfiguration.class, VaultTokenRenewalAutoConfiguration.class))
		.withPropertyValues(applicationProperties())
		.withBean("configClientRestTemplate", RestTemplate.class);

	@Test
	void configurationIsNotEnabledWhenTokenIsMissing() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(VaultTokenRefresher.class));
	}

	@Test
	void configurationIsEnabledWhenTokenIsPresent() {
		contextRunner.withPropertyValues("spring.cloud.config.token=vault-token")
			.run(context -> assertThat(context).hasSingleBean(VaultTokenRefresher.class));
	}

	@Test
	void schedulesVaultTokenRefresh() {
		stubFor(post("/vault/v1/auth/token/renew-self").withHost(equalTo("server.local"))
			.willReturn(aResponse().withHeader("Content-Type", "plain/text").withBody("new-token")));

		contextRunner.withPropertyValues("spring.cloud.config.token=vault-token").run(context -> {
			assertThat(context).hasSingleBean(VaultTokenRefresher.class);

			await().atMost(3L, TimeUnit.SECONDS).untilAsserted(() -> {
				verify(moreThan(2),
						postRequestedFor(urlEqualTo("/vault/v1/auth/token/renew-self"))
							.withHeader("Content-Type", equalTo("application/json"))
							.withHeader("X-Vault-Token", equalTo("vault-token"))
							.withRequestBody(containing("{\"increment\":300}")));
			});
		});
	}

	private String[] applicationProperties() {
		return new String[] { "vault.token.renew.rate=1000", "spring.cloud.config.uri=http://server.local" };
	}

}
