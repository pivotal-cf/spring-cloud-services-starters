/*
 * Copyright 2023-2024 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigClientRequestTemplateFactory;
import org.springframework.cloud.config.client.ConfigServerConfigDataLocationResolver;
import org.springframework.cloud.config.client.ConfigServerConfigDataResource;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static io.pivotal.spring.cloud.config.client.ConfigClientOAuth2Properties.PREFIX;

/**
 * Using oauth2 properties to configure an authorization interceptor for the
 * <code>RestTemplate</code> that calls config server.
 * <p>
 * Note: Despite implementing {@link ConfigDataLocationResolver}, this class does not
 * resolve any location. It only configures and registers the
 * {@link ConfigClientRequestTemplateFactory} which later will be used by
 * {@link ConfigServerConfigDataLocationResolver} to create <code>RestTemplate</code> for
 * calling config server.
 * <p>
 * Finally, it registers the <code>RestTemplate</code> bean to be consumed by
 * {@link ConfigResourceClientAutoConfiguration} and
 * {@link VaultTokenRenewalAutoConfiguration} after application startup.
 */
public class OAuth2ConfigDataLocationResolver
		implements ConfigDataLocationResolver<ConfigServerConfigDataResource>, Ordered {

	private final Log log;

	public OAuth2ConfigDataLocationResolver(DeferredLogFactory factory) {
		this.log = factory.getLog(OAuth2ConfigDataLocationResolver.class);
	}

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext resolverContext, ConfigDataLocation location) {
		if (!location.hasPrefix(ConfigServerConfigDataLocationResolver.PREFIX)) {
			return false;
		}

		var binder = resolverContext.getBinder();
		var isConfigEnabled = binder.bind(ConfigClientProperties.PREFIX + ".enabled", Boolean.class).orElse(true);
		if (!isConfigEnabled) {
			return false;
		}

		var bootstrapContext = resolverContext.getBootstrapContext();

		var oAuth2Properties = binder.bind(PREFIX, ConfigClientOAuth2Properties.class).orElse(null);
		if (oAuth2Properties != null) {
			// Register the custom factory with oauth2 interceptor.
			bootstrapContext.registerIfAbsent(ConfigClientRequestTemplateFactory.class,
					context -> new OAuth2ConfigClientRequestTemplateFactory(this.log,
							context.get(ConfigClientProperties.class), oAuth2Properties));
		}
		else {
			log.warn("Config Client oauth2 properties are missing. Skipping the auth interceptor configuration");
			// Register the default factory.
			bootstrapContext.registerIfAbsent(ConfigClientRequestTemplateFactory.class,
					context -> new ConfigClientRequestTemplateFactory(this.log,
							context.get(ConfigClientProperties.class)));
		}

		bootstrapContext.addCloseListener(event -> {
			// Add the RestTemplate as bean, once the startup is finished.
			event.getApplicationContext()
				.getBeanFactory()
				.registerSingleton("configClientRestTemplate", event.getBootstrapContext().get(RestTemplate.class));

		});

		return false;
	}

	@Override
	public List<ConfigServerConfigDataResource> resolve(ConfigDataLocationResolverContext context,
			ConfigDataLocation location)
			throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
		throw new IllegalStateException("Unexpected call. This resolver should not resolve any location");
	}

	@Override
	public List<ConfigServerConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext context,
			ConfigDataLocation location, Profiles profiles) throws ConfigDataLocationNotFoundException {
		throw new IllegalStateException("Unexpected call. This resolver should not resolve any location");
	}

	/**
	 * It should be registered before {@link ConfigServerConfigDataLocationResolver}. See
	 * {@link ConfigServerConfigDataLocationResolver#getOrder()}
	 */
	@Override
	public int getOrder() {
		return -2;
	}

	private static class OAuth2ConfigClientRequestTemplateFactory extends ConfigClientRequestTemplateFactory {

		private final ClientRegistration clientRegistration;

		public OAuth2ConfigClientRequestTemplateFactory(Log log, ConfigClientProperties clientProperties,
				ConfigClientOAuth2Properties oAuth2Properties) {
			super(log, clientProperties);

			this.clientRegistration = ClientRegistration.withRegistrationId("config-client")
				.clientId(oAuth2Properties.getClientId())
				.clientSecret(oAuth2Properties.getClientSecret())
				.tokenUri(oAuth2Properties.getAccessTokenUri())
				.scope(oAuth2Properties.getScope())
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.build();
		}

		@Override
		public RestTemplate create() {
			var restTemplate = super.create();
			restTemplate.getInterceptors().add(new OAuth2AuthorizedClientHttpRequestInterceptor(clientRegistration));
			return restTemplate;
		}

	}

}
