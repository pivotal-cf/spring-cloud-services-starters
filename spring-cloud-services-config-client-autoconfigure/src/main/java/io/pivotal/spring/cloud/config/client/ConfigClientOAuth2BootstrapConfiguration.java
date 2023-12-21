/*
 * Copyright 2017-2024 the original author or authors.
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ConfigClientProperties.class })
@EnableConfigurationProperties(ConfigClientOAuth2Properties.class)
public class ConfigClientOAuth2BootstrapConfiguration {

	@Bean
	@ConditionalOnMissingBean(ConfigServicePropertySourceLocator.class)
	@ConditionalOnProperty(prefix = "spring.cloud.config.client.oauth2",
			name = { "client-id", "client-secret", "access-token-uri" })
	public ConfigServicePropertySourceLocator configServicePropertySourceLocator(
			ConfigClientProperties configClientProperties, ConfigClientOAuth2Properties configClientOAuth2Properties) {
		ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("config-client")
			.clientId(configClientOAuth2Properties.getClientId())
			.clientSecret(configClientOAuth2Properties.getClientSecret())
			.scope(configClientOAuth2Properties.getScope())
			.tokenUri(configClientOAuth2Properties.getAccessTokenUri())
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.build();
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new OAuth2AuthorizedClientHttpRequestInterceptor(clientRegistration));

		ConfigServicePropertySourceLocator configServicePropertySourceLocator = new ConfigServicePropertySourceLocator(
				configClientProperties);
		configServicePropertySourceLocator.setRestTemplate(restTemplate);
		return configServicePropertySourceLocator;
	}

}
