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

import java.util.ArrayList;

import org.awaitility.Duration;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Roy Clarkson
 * @author Dylan Roberts
 */
public class VaultTokenRenewalAutoConfigurationTest {

	private static final String CLIENT_ID = "clientId";

	private static final String CLIENT_SECRET = "clientSecret";

	private static final String TOKEN_URI = "tokenUri";

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestConfiguration.class, ConfigClientAutoConfiguration.class,
					VaultTokenRenewalAutoConfiguration.class));

	@Test
	public void scheduledVaultTokenRefresh() {
		contextRunner.withPropertyValues("spring.cloud.config.token=footoken", "vault.token.renew.rate=1000",
				"spring.cloud.config.client.oauth2.clientId=" + CLIENT_ID,
				"spring.cloud.config.client.oauth2.clientSecret=" + CLIENT_SECRET,
				"spring.cloud.config.client.oauth2.accessTokenUri=" + TOKEN_URI).run(context -> {
					RestTemplate restTemplate = context.getBean("mockRestTemplate", RestTemplate.class);
					await().atMost(Duration.FIVE_SECONDS).untilAsserted(() -> {
						verify(restTemplate, atLeast(4)).postForObject(anyString(), any(HttpEntity.class), any());
						assertThat(restTemplate.getInterceptors()).hasSize(1);
						assertThat(restTemplate.getInterceptors().get(0))
								.isInstanceOf(OAuth2AuthorizedClientHttpRequestInterceptor.class);
						OAuth2AuthorizedClientHttpRequestInterceptor interceptor = (OAuth2AuthorizedClientHttpRequestInterceptor) restTemplate
								.getInterceptors().get(0);
						ClientRegistration clientRegistration = interceptor.clientRegistration;
						assertThat(clientRegistration.getClientId()).isEqualTo(CLIENT_ID);
						assertThat(clientRegistration.getClientSecret()).isEqualTo(CLIENT_SECRET);
						assertThat(clientRegistration.getProviderDetails().getTokenUri()).isEqualTo(TOKEN_URI);
						assertThat(clientRegistration.getAuthorizationGrantType())
								.isEqualTo(AuthorizationGrantType.CLIENT_CREDENTIALS);
					});
				});
	}

	@Configuration
	public static class TestConfiguration {

		@Qualifier("vaultTokenRenewal")
		@Primary
		@Bean
		public RestTemplate mockRestTemplate() {
			RestTemplate mock = Mockito.mock(RestTemplate.class);
			when(mock.getInterceptors()).thenReturn(new ArrayList<>());
			return mock;
		}

	}

}
