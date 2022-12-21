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

import com.netflix.discovery.EurekaClientConfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.RestTemplateTimeoutProperties;
import org.springframework.cloud.netflix.eureka.config.DiscoveryClientOptionalArgsConfiguration;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * @author Will Tran
 * @author Dylan Roberts
 */
@AutoConfiguration(before = DiscoveryClientOptionalArgsConfiguration.class)
@EnableConfigurationProperties({ EurekaClientOAuth2Properties.class, RestTemplateTimeoutProperties.class })
@ConditionalOnClass({ EurekaClientConfig.class })
@ConditionalOnProperty(prefix = "eureka.client.oauth2", name = { "client-id", "client-secret", "access-token-uri" })
public class EurekaClientOAuth2AutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
	EurekaClientHttpRequestFactorySupplier eurekaClientOAuth2HttpRequestFactorySupplier(
			EurekaClientOAuth2Properties eurekaClientOAuth2Properties,
			RestTemplateTimeoutProperties restTemplateTimeoutProperties) {
		var clientRegistration = ClientRegistration.withRegistrationId("eureka-client")
				.clientId(eurekaClientOAuth2Properties.getClientId())
				.clientSecret(eurekaClientOAuth2Properties.getClientSecret())
				.tokenUri(eurekaClientOAuth2Properties.getAccessTokenUri())
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).build();

		var oAuth2AuthorizedClientHttpRequestInterceptor = new OAuth2AuthorizedClientHttpRequestInterceptor(
				clientRegistration);
		var defaultEurekaClientHttpRequestFactorySupplier = new DefaultEurekaClientHttpRequestFactorySupplier(
				restTemplateTimeoutProperties);

		return new EurekaClientOAuth2HttpRequestFactorySupplier(defaultEurekaClientHttpRequestFactorySupplier,
				oAuth2AuthorizedClientHttpRequestInterceptor);
	}

}
