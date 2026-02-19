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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;

public class OAuth2AccessTokenProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2AccessTokenProvider.class);

	private final AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedManager;

	private final OAuth2AuthorizeRequest authorizeRequest;

	public OAuth2AccessTokenProvider(ClientRegistration registration) {

		var repository = new InMemoryClientRegistrationRepository(registration);
		var service = new InMemoryOAuth2AuthorizedClientService(repository);

		this.authorizedManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(repository, service);

		var authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();
		this.authorizedManager.setAuthorizedClientProvider(authorizedClientProvider);

		this.authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(registration.getRegistrationId())
			.principal(registration.getRegistrationId())
			.build();
	}

	OAuth2AccessToken getAccessToken() {
		OAuth2AuthorizedClient authorizedClient = null;
		try {
			authorizedClient = this.authorizedManager.authorize(this.authorizeRequest);
		}
		catch (OAuth2AuthorizationException ex) {
			LOGGER.error("Authorization error", ex);
		}
		return authorizedClient != null ? authorizedClient.getAccessToken() : null;
	}

}
