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

import com.netflix.discovery.shared.resolver.EurekaEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;

import org.springframework.cloud.netflix.eureka.http.RestTemplateEurekaHttpClient;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * @author Dylan Roberts
 */
public class OAuth2TransportClientFactory extends RestTemplateTransportClientFactory {

	private final ClientRegistration clientRegistration;

	public OAuth2TransportClientFactory(ClientRegistration clientRegistration) {
		this.clientRegistration = clientRegistration;
	}

	@Override
	public EurekaHttpClient newClient(EurekaEndpoint serviceUrl) {
		return new RestTemplateEurekaHttpClient(restTemplate(serviceUrl.getServiceUrl()), serviceUrl.getServiceUrl());
	}

	private RestTemplate restTemplate(String serviceUrl) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new OAuth2AuthorizedClientHttpRequestInterceptor(clientRegistration));
		restTemplate.getMessageConverters().add(0, mappingJacksonHttpMessageConverter());
		restTemplate.setErrorHandler(new ErrorHandler());

		return restTemplate;
	}

	/*
	 * copy/pasted from super class would be nice to expose this class as protected in
	 * super class
	 */
	class ErrorHandler extends DefaultResponseErrorHandler {

		@Override
		protected boolean hasError(HttpStatus statusCode) {
			/**
			 * When the Eureka server restarts and a client tries to sent a heartbeat the
			 * server will respond with a 404. By default RestTemplate will throw an
			 * exception in this case. What we want is to return the 404 to the upstream
			 * code so it will send another registration request to the server.
			 */
			if (statusCode.is4xxClientError()) {
				return false;
			}
			return super.hasError(statusCode);
		}

	}

}
