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

import java.util.List;

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestTemplate;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

/**
 * Using {@link CfEnv} directly as a {@link BootstrapRegistryInitializer} is required to
 * setup the RestTemplate that calls config-server. There's presently no earlier extension
 * point that java-cfenv library can use to setup the properties before this is called.
 *
 * @author Dylan Roberts
 */
public class ConfigClientOAuth2BootstrapRegistryInitializer implements BootstrapRegistryInitializer {

	private static final boolean CONFIG_CLIENT_IS_PRESENT = ClassUtils
			.isPresent("org.springframework.cloud.config.client.ConfigServerConfigDataLoader", null);

	private static final boolean OAUTH2_CLIENT_IS_PRESENT = ClassUtils
			.isPresent("org.springframework.security.oauth2.client.registration.ClientRegistration", null);

	private static final boolean JAVA_CFENV_IS_PRESENT = ClassUtils.isPresent("io.pivotal.cfenv.core.CfEnv", null);

	@Override
	public void initialize(BootstrapRegistry registry) {
		if (!CONFIG_CLIENT_IS_PRESENT || !OAUTH2_CLIENT_IS_PRESENT || !JAVA_CFENV_IS_PRESENT)
			return;

		CfEnv cfEnv = new CfEnv();
		List<CfService> configServices = cfEnv.findServicesByTag("configuration");
		if (configServices.size() != 1)
			return;
		CfCredentials credentials = configServices.stream().findFirst().get().getCredentials();

		registry.register(RestTemplate.class, context -> {
			String clientId = credentials.getString("client_id");
			String clientSecret = credentials.getString("client_secret");
			String accessTokenUri = credentials.getString("access_token_uri");
			RestTemplate restTemplate = new RestTemplate();
			ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("config-client")
					.clientId(clientId).clientSecret(clientSecret).tokenUri(accessTokenUri)
					.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).build();
			restTemplate.getInterceptors().add(new OAuth2AuthorizedClientHttpRequestInterceptor(clientRegistration));
			return restTemplate;
		});
	}

}
