/*
 * Copyright 2017-2019 the original author or authors.
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

import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.cloud.config.client.ConfigClientProperties.TOKEN_HEADER;

/**
 * {@link RestTemplate} based implementation of {@link ConfigResourceClient}. Config
 * Server URI, default application name, profiles and labels are provided by
 * {@link ConfigClientProperties}.
 *
 * @author Daniel Lavoie
 * @author Anshul Mehra
 */
class OAuth2ConfigResourceClient implements ConfigResourceClient {

	private enum ResourceType {

		BINARY, PLAINTEXT

	}

	private final ConfigClientProperties configClientProperties;

	private final RestTemplate restTemplate;

	protected OAuth2ConfigResourceClient(RestTemplate restTemplate,
			final ConfigClientProperties configClientProperties) {
		this.restTemplate = restTemplate;
		this.configClientProperties = configClientProperties;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Resource getPlainTextResource(String path) {
		return getPlainTextResource(null, null, path);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Resource getPlainTextResource(String profile, String label, String path) {
		return getResource(profile, label, path, ResourceType.PLAINTEXT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Resource getBinaryResource(String path) {
		return getBinaryResource(null, null, path);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Resource getBinaryResource(String profile, String label, String path) {
		return getResource(profile, label, path, ResourceType.BINARY);
	}

	private Resource getResource(String profile, String label, String path, ResourceType resourceType) {
		Assert.isTrue(configClientProperties.getName() != null && !configClientProperties.getName().isEmpty(),
				"Spring application name is undefined.");

		Assert.notEmpty(configClientProperties.getUri(), "Config server URI is undefined");
		Assert.hasText(configClientProperties.getUri()[0], "Config server URI is undefined.");

		if (profile == null) {
			profile = configClientProperties.getProfile();
			if (profile == null || profile.isEmpty()) {
				profile = "default";
			}
		}

		if (label == null) {
			label = configClientProperties.getLabel();
		}

		UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(configClientProperties.getUri()[0])
				.pathSegment(configClientProperties.getName()).pathSegment(profile).pathSegment(label)
				.pathSegment(path);
		if (label == null) {
			urlBuilder.queryParam("useDefaultLabel");
		}
		RequestEntity.HeadersBuilder<?> requestBuilder = RequestEntity.get(urlBuilder.build().toUri());
		if (StringUtils.hasText(configClientProperties.getToken())) {
			requestBuilder.header(TOKEN_HEADER, configClientProperties.getToken());
		}
		if (resourceType == ResourceType.BINARY) {
			requestBuilder.accept(MediaType.APPLICATION_OCTET_STREAM);
		}

		ResponseEntity<Resource> forEntity = restTemplate.exchange(requestBuilder.build(), Resource.class);
		return forEntity.getBody();
	}

}
