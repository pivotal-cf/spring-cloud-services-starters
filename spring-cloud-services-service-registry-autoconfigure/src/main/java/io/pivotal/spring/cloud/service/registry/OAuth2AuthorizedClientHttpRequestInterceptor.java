/*
 * Copyright 2019-2024 the original author or authors.
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
package io.pivotal.spring.cloud.service.registry;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * {@link ClientHttpRequestInterceptor} implementation to add authorization header to
 * request based on an {@link OAuth2AuthorizedClient}.
 *
 * @author Dylan Roberts
 */
public class OAuth2AuthorizedClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	private final AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedManager;

	private final OAuth2AuthorizeRequest authorizeRequest;

	public OAuth2AuthorizedClientHttpRequestInterceptor(ClientRegistration registration) {

		var repository = new InMemoryClientRegistrationRepository(registration);
		var service = new InMemoryOAuth2AuthorizedClientService(repository);

		this.authorizedManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(repository, service);

		var authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();
		this.authorizedManager.setAuthorizedClientProvider(authorizedClientProvider);

		this.authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(registration.getRegistrationId())
			.principal(registration.getRegistrationId())
			.build();
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {

		OAuth2AuthorizedClient authorize = this.authorizedManager.authorize(this.authorizeRequest);
		if (authorize != null) {
			request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + authorize.getAccessToken().getTokenValue());
		}

		return execution.execute(request, body);
	}

}
