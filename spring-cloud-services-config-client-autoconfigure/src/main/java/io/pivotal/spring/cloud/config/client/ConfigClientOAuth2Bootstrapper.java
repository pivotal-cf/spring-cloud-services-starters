/*
 * Copyright 2021 the original author or authors.
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

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistry.InstanceSupplier;
import org.springframework.boot.Bootstrapper;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestTemplate;

/**
 * @author Dylan Roberts
 */
public class ConfigClientOAuth2Bootstrapper implements Bootstrapper {

	static final boolean CONFIG_CLIENT_IS_PRESENT = ClassUtils
			.isPresent("org.springframework.cloud.config.client.ConfigServerConfigDataLoader", null);

	@Override
	public void intitialize(BootstrapRegistry registry) {
		if (!CONFIG_CLIENT_IS_PRESENT)
			return;

		registry.register(ConfigClientOAuth2Properties.class,
				context -> context.get(Binder.class)
						.bind(ConfigClientOAuth2Properties.PREFIX, ConfigClientOAuth2Properties.class)
						.orElseGet(ConfigClientOAuth2Properties::new));

		registry.register(RestTemplate.class, context -> {
			ConfigClientOAuth2Properties configClientOAuth2Properties = context.get(ConfigClientOAuth2Properties.class);
			RestTemplate restTemplate = new RestTemplate();
			ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("config-client")
					.clientId(configClientOAuth2Properties.getClientId())
					.clientSecret(configClientOAuth2Properties.getClientSecret())
					.tokenUri(configClientOAuth2Properties.getAccessTokenUri())
					.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).build();
			restTemplate.getInterceptors().add(new OAuth2AuthorizedClientHttpRequestInterceptor(clientRegistration));
			return restTemplate;
		});
	}

}
