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

import javax.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * @author Mike Heath
 * @author Will Tran
 * @author Dylan Roberts
 */
@Configuration
@ConditionalOnClass({ ConfigServicePropertySourceLocator.class })
@ConditionalOnProperty(prefix = "spring.cloud.config.client.oauth2",
		name = { "client-id", "client-secret", "access-token-uri" })
@EnableConfigurationProperties(ConfigClientOAuth2Properties.class)
public class ConfigClientOAuth2BootstrapConfiguration {

	private final ConfigServicePropertySourceLocator locator;

	private final ConfigClientOAuth2Properties configClientOAuth2Properties;

	public ConfigClientOAuth2BootstrapConfiguration(ConfigServicePropertySourceLocator locator,
			ConfigClientOAuth2Properties configClientOAuth2Properties) {
		Assert.notNull(locator, "Error injecting ConfigServicePropertySourceLocator, this can occur"
				+ "using self signed certificates in Cloud Foundry without setting the TRUST_CERTS environment variable");
		this.locator = locator;
		this.configClientOAuth2Properties = configClientOAuth2Properties;
	}

	@PostConstruct
	public void init() {
		RestTemplate restTemplate = new RestTemplate();
		ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("config-client")
				.clientId(configClientOAuth2Properties.getClientId())
				.clientSecret(configClientOAuth2Properties.getClientSecret())
				.tokenUri(configClientOAuth2Properties.getAccessTokenUri())
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).build();
		restTemplate.getInterceptors().add(new OAuth2AuthorizedClientHttpRequestInterceptor(clientRegistration));
		locator.setRestTemplate(restTemplate);
	}

}
