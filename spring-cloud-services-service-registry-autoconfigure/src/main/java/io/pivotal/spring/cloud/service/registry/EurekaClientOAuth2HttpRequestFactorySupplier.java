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

public class EurekaClientOAuth2HttpRequestFactorySupplier implements EurekaClientHttpRequestFactorySupplier {

	private final OAuth2AuthorizedClientHttpRequestInterceptor interceptor;

	public EurekaClientOAuth2HttpRequestFactorySupplier(OAuth2AuthorizedClientHttpRequestInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public ClientHttpRequestFactory get(SSLContext sslContext, HostnameVerifier hostnameVerifier) {
		var defaultSupplier = new DefaultEurekaClientHttpRequestFactorySupplier();
		var defaultFactory = defaultSupplier.get(sslContext, hostnameVerifier);

		return new InterceptingClientHttpRequestFactory(defaultFactory, List.of(interceptor));
	}

}
