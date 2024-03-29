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

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Dylan Roberts
 */
@ConfigurationProperties(prefix = ConfigClientOAuth2Properties.PREFIX)
public class ConfigClientOAuth2Properties {

	protected static final String PREFIX = "spring.cloud.config.client.oauth2";

	private String clientId;

	private String clientSecret;

	private String accessTokenUri;

	private Set<String> scope;

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getAccessTokenUri() {
		return accessTokenUri;
	}

	public void setAccessTokenUri(String accessTokenUri) {
		this.accessTokenUri = accessTokenUri;
	}

	public Set<String> getScope() {
		return scope;
	}

	public void setScope(Set<String> scope) {
		this.scope = scope;
	}

}
