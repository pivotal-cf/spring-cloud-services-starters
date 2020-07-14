/*
 * Copyright 2020 the original author or authors.
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

import java.util.Collection;
import java.util.Optional;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.TransportClientFactory;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;

import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * @author Dylan Roberts
 */
public class OAuth2TransportClientFactories implements TransportClientFactories<Void> {

	private final ClientRegistration clientRegistration;

	public OAuth2TransportClientFactories(ClientRegistration clientRegistration) {
		this.clientRegistration = clientRegistration;
	}

	@Override
	@Deprecated
	public TransportClientFactory newTransportClientFactory(Collection<Void> additionalFilters,
			EurekaJerseyClient providedJerseyClient) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TransportClientFactory newTransportClientFactory(EurekaClientConfig clientConfig,
			Collection<Void> additionalFilters, InstanceInfo myInstanceInfo) {
		return new OAuth2TransportClientFactory(clientRegistration);
	}

	@Override
	public TransportClientFactory newTransportClientFactory(EurekaClientConfig clientConfig,
			Collection<Void> additionalFilters, InstanceInfo myInstanceInfo, Optional<SSLContext> sslContext,
			Optional<HostnameVerifier> hostnameVerifier) {
		return new OAuth2TransportClientFactory(clientRegistration);
	}

}
