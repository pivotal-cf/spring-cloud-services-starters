/*
 * Copyright 2022 the original author or authors.
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

import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.util.List;

/**
 * {@link EurekaClientHttpRequestFactorySupplier} implementation to add authorization
 * interceptor to the {@link ClientHttpRequestFactory}.
 */
public class EurekaClientOAuth2HttpRequestFactorySupplier implements EurekaClientHttpRequestFactorySupplier {

	private final EurekaClientHttpRequestFactorySupplier defaultEurekaClientHttpRequestFactorySupplier;

	private final OAuth2AuthorizedClientHttpRequestInterceptor oAuth2AuthorizedClientHttpRequestInterceptor;

	public EurekaClientOAuth2HttpRequestFactorySupplier(
			DefaultEurekaClientHttpRequestFactorySupplier defaultEurekaClientHttpRequestFactorySupplier,
			OAuth2AuthorizedClientHttpRequestInterceptor oAuth2AuthorizedClientHttpRequestInterceptor) {
		this.defaultEurekaClientHttpRequestFactorySupplier = defaultEurekaClientHttpRequestFactorySupplier;
		this.oAuth2AuthorizedClientHttpRequestInterceptor = oAuth2AuthorizedClientHttpRequestInterceptor;
	}

	@Override
	public ClientHttpRequestFactory get(SSLContext sslContext, HostnameVerifier hostnameVerifier) {
		var clientHttpRequestFactory = defaultEurekaClientHttpRequestFactorySupplier.get(sslContext, hostnameVerifier);

		return new InterceptingClientHttpRequestFactory(clientHttpRequestFactory,
				List.of(oAuth2AuthorizedClientHttpRequestInterceptor));
	}

}
