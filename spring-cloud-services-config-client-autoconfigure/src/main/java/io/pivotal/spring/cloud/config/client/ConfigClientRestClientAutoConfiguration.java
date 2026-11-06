/*
 * Copyright 2019-2026 the original author or authors.
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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * Provides the {@code configClientRestClient} bean used to call the config server outside
 * of config data loading (for example Vault token renewal). When
 * {@code spring.cloud.config.oauth2.enabled} is true, requests carry an access token from
 * the client registration named by
 * {@code spring.cloud.config.oauth2.client-registration-id}.
 */
@AutoConfiguration(before = VaultTokenRenewalAutoConfiguration.class,
		after = { ConfigClientAutoConfiguration.class, OAuth2ClientAutoConfiguration.class })
@ConditionalOnClass(ConfigClientProperties.class)
public class ConfigClientRestClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "configClientRestClient")
	public RestClient configClientRestClient(ObjectProvider<ClientRegistrationRepository> clientRegistrations,
			Environment environment) {
		var builder = RestClient.builder();
		var registrationId = environment.getProperty("spring.cloud.config.oauth2.client-registration-id");
		var repository = clientRegistrations.getIfAvailable();
		if (environment.getProperty("spring.cloud.config.oauth2.enabled", Boolean.class, false) && repository != null
				&& registrationId != null) {
			builder.requestInterceptor(oauth2Interceptor(repository, registrationId));
		}
		return builder.build();
	}

	private static OAuth2ClientHttpRequestInterceptor oauth2Interceptor(ClientRegistrationRepository repository,
			String registrationId) {
		var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(repository,
				new InMemoryOAuth2AuthorizedClientService(repository));
		manager
			.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
		var interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
		interceptor.setClientRegistrationIdResolver(request -> registrationId);
		return interceptor;
	}

}
