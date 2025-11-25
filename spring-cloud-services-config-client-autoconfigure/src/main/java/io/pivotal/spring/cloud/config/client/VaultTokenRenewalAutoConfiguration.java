/*
 * Copyright 2019-2024 the original author or authors.
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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for a periodic Vault token renewer. Conditionally configured if there is
 * a {@link ConfigClientProperties} bean and if there is a `spring.cloud.config.token`
 * property set. By default, the token is renewed every 60 seconds and is renewed with a 5
 * minute time-to-live. The renewal rate can be configured by setting
 * `vault.token.renew.rate` to some value that is the renewal rate in milliseconds. The
 * renewal time-to-live can be specified with by setting `vault.token.ttl` to some value
 * indicating the time-to-live in milliseconds.
 *
 * @author cwalls
 */
@AutoConfiguration(after = ConfigClientAutoConfiguration.class)
@ConditionalOnBean(ConfigClientProperties.class)
@ConditionalOnProperty(name = "spring.cloud.config.token")
@EnableConfigurationProperties
@EnableScheduling
public class VaultTokenRenewalAutoConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(VaultTokenRenewalAutoConfiguration.class);

	private static final String VAULT_TOKEN_HEADER = "X-Vault-Token";

	private static final String REFRESH_PATH = "/vault/v1/auth/token/renew-self";

	// Default to a 300 second (5 minute) TTL
	@Value("${vault.token.ttl:300000}")
	long ttl;

	@Bean
	@ConditionalOnBean(value = RestTemplate.class, name = "configClientRestTemplate")
	public VaultTokenRefresher vaultTokenRefresher(
			@Qualifier("configClientRestTemplate") RestTemplate configClientRestTemplate,
			ConfigClientProperties configClientProperties) {

		var refreshUri = configClientProperties.getUri()[0] + REFRESH_PATH;
		String vaultToken = configClientProperties.getToken();
		var obscuredToken = vaultToken.substring(0, 4) + "[*]" + vaultToken.substring(vaultToken.length() - 4);

		return new VaultTokenRefresher(configClientRestTemplate, obscuredToken, ttl, refreshUri,
				buildTokenRenewRequest(vaultToken));
	}

	private HttpEntity<Map<String, Long>> buildTokenRenewRequest(String vaultToken) {
		// convert to seconds, since that's what Vault wants
		var ttlInSeconds = this.ttl / 1000;
		var requestBody = Map.of("increment", ttlInSeconds);
		var headers = new HttpHeaders();
		headers.set(VAULT_TOKEN_HEADER, vaultToken);
		headers.setContentType(MediaType.APPLICATION_JSON);

		return new HttpEntity<>(requestBody, headers);
	}

	public static class VaultTokenRefresher {

		private final String obscuredToken;

		private final long ttl;

		private final String refreshUri;

		private final HttpEntity<Map<String, Long>> request;

		private final RestTemplate restTemplate;

		VaultTokenRefresher(RestTemplate restTemplate, String obscuredToken, long ttl, String refreshUri,
				HttpEntity<Map<String, Long>> request) {
			this.restTemplate = restTemplate;
			this.obscuredToken = obscuredToken;
			this.ttl = ttl;
			this.refreshUri = refreshUri;
			this.request = request;
		}

		// Default to renew token every 60 seconds
		@Scheduled(fixedRateString = "${vault.token.renew.rate:60000}")
		public void refreshVaultToken() {
			try {
				LOGGER.debug("Renewing Vault token " + obscuredToken + " for " + ttl + " milliseconds.");
				restTemplate.postForObject(refreshUri, request, String.class);
			}
			catch (RestClientException e) {
				LOGGER.error("Unable to renew Vault token {}. Is the token invalid or expired?", obscuredToken);
			}
		}

	}

}
