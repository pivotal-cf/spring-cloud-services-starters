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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import static org.springframework.cloud.config.client.ConfigClientProperties.AUTHORIZATION;

/**
 * Using oauth2 properties to configure an authorization interceptor for the
 * <code>RestTemplate</code> that calls config server.
 * <p>
 * Note: Despite implementing {@link ConfigDataLocationResolver}, this class does not
 * resolve any location. It only configures and registers the
 * {@link ConfigClientRequestTemplateFactory} and <code>RestTemplate</code> which later
 * will be used by {@link ConfigServerConfigDataLocationResolver} for calling config
 * server.
 * <p>
 * Finally, it registers the <code>RestTemplate</code> and <code>RestClient</code> beans
 * to be consumed by {@link ConfigResourceClientAutoConfiguration} and
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

		var oAuth2Properties = binder.bind(ConfigClientOAuth2Properties.PREFIX, ConfigClientOAuth2Properties.class)
			.orElse(null);
		var clientProperties = binder.bind(ConfigClientProperties.PREFIX, ConfigClientProperties.class)
			.orElse(new ConfigClientProperties(new StandardEnvironment()));

		var tokenProvider = buildAccessTokenProvider(oAuth2Properties);

		// Register the custom factory with oauth2 interceptor.
		bootstrapContext.registerIfAbsent(ConfigClientRequestTemplateFactory.class,
				context -> new OAuth2ConfigClientRequestTemplateFactory(this.log, clientProperties, tokenProvider));
		var factory = (OAuth2ConfigClientRequestTemplateFactory) bootstrapContext
			.get(ConfigClientRequestTemplateFactory.class);
		// Update the factory, in case it was registered earlier
		factory.update(clientProperties, tokenProvider);

		// Register the template with oauth2 interceptor.
		bootstrapContext.registerIfAbsent(RestTemplate.class, context -> factory.create());
		var template = bootstrapContext.get(RestTemplate.class);
		// Update the template, in case it was registered earlier
		factory.updateTemplate(template);

		// Add the RestTemplate RestClient, and OAuth2AccessTokenProvider as beans, once
		// the startup is finished.
		bootstrapContext.addCloseListener(event -> {
			var beanFactory = event.getApplicationContext().getBeanFactory();
			// Avoid duplicate registration
			if (tokenProvider != null && !beanFactory.containsBean("configClientAccessTokenProvider")) {
				beanFactory.registerSingleton("configClientAccessTokenProvider", tokenProvider);
			}

			// Avoid duplicate registration
			if (!beanFactory.containsBean("configClientRestClient")) {
				var eventBootstrapContext = event.getBootstrapContext();
				var restTemplate = eventBootstrapContext.get(RestTemplate.class);
				beanFactory.registerSingleton("configClientRestClient", RestClient.create(restTemplate));

				// Legacy
				beanFactory.registerSingleton("configClientRestTemplate", restTemplate);
			}
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

	private OAuth2AccessTokenProvider buildAccessTokenProvider(ConfigClientOAuth2Properties oAuth2Properties) {
		if (oAuth2Properties == null) {
			return null;
		}

		return new OAuth2AccessTokenProvider(ClientRegistration.withRegistrationId("config-client")
			.clientId(oAuth2Properties.getClientId())
			.clientSecret(oAuth2Properties.getClientSecret())
			.tokenUri(oAuth2Properties.getAccessTokenUri())
			.scope(oAuth2Properties.getScope())
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.build());
	}

	private static class OAuth2ConfigClientRequestTemplateFactory extends ConfigClientRequestTemplateFactory {

		private ConfigClientProperties properties;

		private OAuth2AccessTokenProvider tokenProvider;

		public OAuth2ConfigClientRequestTemplateFactory(Log log, ConfigClientProperties clientProperties,
				OAuth2AccessTokenProvider tokenProvider) {
			super(log, clientProperties);

			this.properties = clientProperties;
			this.tokenProvider = tokenProvider;
		}

		public void update(ConfigClientProperties clientProperties, OAuth2AccessTokenProvider tokenProvider) {
			this.properties = clientProperties;
			this.tokenProvider = tokenProvider;
		}

		@Override
		public ConfigClientProperties getProperties() {
			return this.properties;
		}

		@Override
		public RestTemplate create() {
			if (this.properties.getRequestReadTimeout() < 0) {
				throw new IllegalStateException("Invalid Value for Read Timeout set.");
			}
			if (this.properties.getRequestConnectTimeout() < 0) {
				throw new IllegalStateException("Invalid Value for Connect Timeout set.");
			}

			return updateTemplate(new RestTemplate());
		}

		@Override
		public void addAuthorizationToken(HttpHeaders httpHeaders, String username, String password) {
			String authorization = this.properties.getHeaders().get(AUTHORIZATION);

			if (password != null && authorization != null) {
				throw new IllegalStateException("You must set either 'password' or 'authorization'");
			}

			if (password != null) {
				byte[] token = java.util.Base64.getEncoder()
					.encode((username + ":" + password).getBytes(StandardCharsets.UTF_8));
				httpHeaders.add("Authorization", "Basic " + new String(token, StandardCharsets.UTF_8));
			}
			else if (authorization != null) {
				httpHeaders.add("Authorization", authorization);
			}
		}

		RestTemplate updateTemplate(RestTemplate template) {
			template.setRequestFactory(createHttpRequestFactory(this.properties));

			var interceptors = new ArrayList<ClientHttpRequestInterceptor>();

			var headers = new HashMap<>(this.properties.getHeaders());
			headers.remove(AUTHORIZATION); // To avoid redundant addition of header
			if (!headers.isEmpty()) {
				interceptors.add(new GenericRequestHeaderInterceptor(headers));
			}

			if (this.tokenProvider != null) {
				interceptors.add(new OAuth2AuthorizedClientHttpRequestInterceptor(this.tokenProvider));
			}

			template.setInterceptors(interceptors);
			return template;
		}

	}

}
