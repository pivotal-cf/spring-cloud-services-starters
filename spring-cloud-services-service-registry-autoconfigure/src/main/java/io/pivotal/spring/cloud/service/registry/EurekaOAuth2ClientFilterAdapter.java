/*
 * Copyright 2019 the original author or authors.
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

import java.time.Clock;
import java.time.Instant;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

/**
 *
 * @author Will Tran
 * @author Dylan Roberts
 *
 */
public class EurekaOAuth2ClientFilterAdapter extends ClientFilter {

	private final ClientRegistration clientRegistration;
	private OAuth2AccessToken accessToken;

	public EurekaOAuth2ClientFilterAdapter(ClientRegistration clientRegistration) {
		this.clientRegistration = clientRegistration;
	}

	@Override
	public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
		Instant now = Clock.systemUTC().instant();
		if (accessToken == null || now.isAfter(accessToken.getExpiresAt())) {
			DefaultClientCredentialsTokenResponseClient tokenResponseClient = new DefaultClientCredentialsTokenResponseClient();
			OAuth2ClientCredentialsGrantRequest clientCredentialsGrantRequest = new OAuth2ClientCredentialsGrantRequest(clientRegistration);
			accessToken = tokenResponseClient.getTokenResponse(clientCredentialsGrantRequest).getAccessToken();
		}

		cr.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getTokenValue());

		return getNext().handle(cr);
	}

}
