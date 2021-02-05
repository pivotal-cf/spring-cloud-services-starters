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

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dylan Roberts
 */
class ConfigClientOAuth2BootstrapperTest {

	private static final String CLIENT_ID = "clientId123";

	private static final String CLIENT_SECRET = "clientSecret123";

	private static final String ACCESS_TOKEN_URI = "accessTokenUri123";

	@Test
	void shouldCreateOAuth2EnabledRestTemplateInBootstrapContext() {
		ConfigurableApplicationContext context = null;
		try {
			context = new SpringApplicationBuilder(TestConfig.class)
					.addBootstrapper(new ConfigClientOAuth2Bootstrapper())
					.addBootstrapper(registry -> registry.addCloseListener(event -> {
						RestTemplate restTemplate = event.getBootstrapContext().get(RestTemplate.class);
						assertThat(restTemplate).isNotNull();
						assertThat(restTemplate.getInterceptors()).hasSize(1);
						assertThat(restTemplate.getInterceptors().get(0))
								.isInstanceOf(OAuth2AuthorizedClientHttpRequestInterceptor.class);
						OAuth2AuthorizedClientHttpRequestInterceptor interceptor = (OAuth2AuthorizedClientHttpRequestInterceptor) restTemplate
								.getInterceptors().get(0);
						ClientRegistration clientRegistration = interceptor.clientRegistration;
						assertThat(clientRegistration.getClientId()).isEqualTo(CLIENT_ID);
						assertThat(clientRegistration.getClientSecret()).isEqualTo(CLIENT_SECRET);
						assertThat(clientRegistration.getProviderDetails().getTokenUri()).isEqualTo(ACCESS_TOKEN_URI);
						assertThat(clientRegistration.getAuthorizationGrantType())
								.isEqualTo(AuthorizationGrantType.CLIENT_CREDENTIALS);
					})).run("--spring.config.import=optional:configserver:", "--spring.cloud.config.enabled=true",
							"--spring.cloud.config.client.oauth2.client-id=" + CLIENT_ID,
							"--spring.cloud.config.client.oauth2.client-secret=" + CLIENT_SECRET,
							"--spring.cloud.config.client.oauth2.access-token-uri=" + ACCESS_TOKEN_URI);
		}
		finally {
			if (context != null) {
				context.close();
			}
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestConfig {

	}

}
