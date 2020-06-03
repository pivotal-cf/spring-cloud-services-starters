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
package io.pivotal.spring.cloud.config.client;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

/**
 * {@link ClientHttpRequestInterceptor} implementation to add authorization header to
 * request based on an {@link OAuth2AuthorizedClient}.
 *
 * @author Dylan Roberts
 */
public class OAuth2AuthorizedClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    final ClientRegistration clientRegistration;

    private OAuth2AccessToken accessToken;

    public OAuth2AuthorizedClientHttpRequestInterceptor(ClientRegistration clientRegistration) {
        this.clientRegistration = clientRegistration;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        Instant now = Clock.systemUTC().instant();
        if (accessToken == null || now.isAfter(accessToken.getExpiresAt())) {
            DefaultClientCredentialsTokenResponseClient tokenResponseClient = new DefaultClientCredentialsTokenResponseClient();
            OAuth2ClientCredentialsGrantRequest clientCredentialsGrantRequest = new OAuth2ClientCredentialsGrantRequest(clientRegistration);
            accessToken = tokenResponseClient.getTokenResponse(clientCredentialsGrantRequest).getAccessToken();
        }

        request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getTokenValue());
        return execution.execute(request, body);
    }

}
