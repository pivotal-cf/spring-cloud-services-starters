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

import com.netflix.discovery.DiscoveryClient.DiscoveryClientOptionalArgs;
import com.netflix.discovery.EurekaClientConfig;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.config.DiscoveryClientOptionalArgsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * @author Will Tran
 * @author Dylan Roberts
 *
 */
@Configuration
@EnableConfigurationProperties(EurekaClientOAuth2Properties.class)
@ConditionalOnClass({ EurekaClientConfig.class })
@ConditionalOnProperty(prefix = "eureka.client.oauth2", name = { "client-id", "client-secret", "access-token-uri" })
@AutoConfigureBefore(DiscoveryClientOptionalArgsConfiguration.class)
public class EurekaClientOAuth2AutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(DiscoveryClientOptionalArgs.class)
	public OAuth2DiscoveryClientOptionalArgs discoveryClientOptionalArgs(
			EurekaClientOAuth2Properties eurekaClientOAuth2Properties) {
		ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("eureka-client")
				.clientId(eurekaClientOAuth2Properties.getClientId())
				.clientSecret(eurekaClientOAuth2Properties.getClientSecret())
				.scope(eurekaClientOAuth2Properties.getScope())
				.tokenUri(eurekaClientOAuth2Properties.getAccessTokenUri())
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).build();
		return new OAuth2DiscoveryClientOptionalArgs(clientRegistration);
	}

}
