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

package io.pivotal.spring.cloud.oauth2.client;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * {@link ClientHttpRequestInterceptor} implementation to add authorization header to
 * request based on an {@link OAuth2AccessTokenProvider}.
 *
 * @author Dylan Roberts
 */
public class OAuth2AuthorizedClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	private final OAuth2AccessTokenProvider tokenProvider;

	public OAuth2AuthorizedClientHttpRequestInterceptor(OAuth2AccessTokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {

		var token = this.tokenProvider.getAccessToken();
		if (token != null) {
			request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token.getTokenValue());
		}

		return execution.execute(request, body);
	}

}
