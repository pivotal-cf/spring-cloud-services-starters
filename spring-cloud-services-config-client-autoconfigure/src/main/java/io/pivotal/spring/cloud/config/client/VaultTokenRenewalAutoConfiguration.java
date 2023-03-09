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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for a periodic Vault token renewer. Conditionally configured if there is
 * a {@link ConfigClientProperties} bean and if there is a `spring.cloud.config.token`
 * property set.
 *
 * By default, the token is renewed every 60 seconds and is renewed with a 5 minute
 * time-to-live. The renew rate can be configured by setting `vault.token.renew.rate` to
 * some value that is the renewal rate in milliseconds. The renewal time-to-live can be
 * specified with by setting `vault.token.ttl` to some value indicating the time-to-live
 * in milliseconds.
 *
 * @author cwalls
 */
@Configuration
@ConditionalOnBean(ConfigClientProperties.class)
@ConditionalOnProperty(prefix = "spring.cloud.config",
		name = { "token", "client.oauth2.clientId", "client.oauth2.clientSecret", "client.oauth2.accessTokenUri" })
@AutoConfigureAfter(ConfigClientAutoConfiguration.class)
@EnableConfigurationProperties(ConfigClientOAuth2Properties.class)
@EnableScheduling
public class VaultTokenRenewalAutoConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(VaultTokenRenewalAutoConfiguration.class);

	@Bean
	public VaultTokenRefresher vaultTokenRefresher(ConfigClientProperties configClientProperties,
			ConfigClientOAuth2Properties configClientOAuth2Properties,
			@Qualifier("vaultTokenRenewal") RestTemplate restTemplate,
			@Value("${spring.cloud.config.token}") String vaultToken,
			// Default to a 300 second (5 minute) TTL
			@Value("${vault.token.ttl:300000}") long renewTTL) {
		ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("config-client")
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.clientId(configClientOAuth2Properties.getClientId())
				.clientSecret(configClientOAuth2Properties.getClientSecret())
				.scope(configClientOAuth2Properties.getScope())
				.tokenUri(configClientOAuth2Properties.getAccessTokenUri()).build();
		restTemplate.getInterceptors().add(new OAuth2AuthorizedClientHttpRequestInterceptor(clientRegistration));
		String obscuredToken = vaultToken.substring(0, 4) + "[*]" + vaultToken.substring(vaultToken.length() - 4);
		String refreshUri = configClientProperties.getUri()[0] + "/vault/v1/auth/token/renew-self";
		// convert to seconds, since that's what Vault wants
		long renewTTLInMS = renewTTL / 1000;
		HttpEntity<Map<String, Long>> request = buildTokenRenewRequest(vaultToken, renewTTLInMS);
		return new VaultTokenRefresher(restTemplate, obscuredToken, renewTTL, refreshUri, request);
	}

	@Bean("vaultTokenRenewal")
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	private HttpEntity<Map<String, Long>> buildTokenRenewRequest(String vaultToken, long renewTTL) {
		Map<String, Long> requestBody = new HashMap<>();
		requestBody.put("increment", renewTTL);
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Vault-Token", vaultToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		return new HttpEntity<>(requestBody, headers);
	}

	static class VaultTokenRefresher {

		private final String obscuredToken;

		private final long renewTTL;

		private final String refreshUri;

		private final HttpEntity<Map<String, Long>> request;

		private final RestTemplate restTemplate;

		VaultTokenRefresher(RestTemplate restTemplate, String obscuredToken, long renewTTL, String refreshUri,
				HttpEntity<Map<String, Long>> request) {
			this.restTemplate = restTemplate;
			this.obscuredToken = obscuredToken;
			this.renewTTL = renewTTL;
			this.refreshUri = refreshUri;
			this.request = request;
		}

		// Default to renew token every 60 seconds
		@Scheduled(fixedRateString = "${vault.token.renew.rate:60000}")
		public void refreshVaultToken() {
			try {
				LOGGER.debug("Renewing Vault token " + obscuredToken + " for " + renewTTL + " milliseconds.");
				restTemplate.postForObject(refreshUri, request, String.class);
			}
			catch (RestClientException e) {
				LOGGER.error("Unable to renew Vault token " + obscuredToken + ". Is the token invalid or expired?");
			}
		}

	}

}
