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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.cloud.config.client.ConfigClientRequestTemplateFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2ConfigDataLocationResolverTest {

	private MockEnvironment environment;

	DefaultBootstrapContext bootstrapContext;

	private Binder binder;

	@Mock
	private ConfigDataLocationResolverContext context;

	private final ConfigDataLocationResolver<?> resolver = new OAuth2ConfigDataLocationResolver(new DeferredLogs());

	@BeforeEach
	void setup() {
		this.environment = new MockEnvironment();
		this.bootstrapContext = new DefaultBootstrapContext();
		this.binder = Binder.get(environment);
	}

	@Test
	void shouldNotRegisterAnyBeanWhenLocationIsNotConfigServer() {

		assertThat(resolver.isResolvable(context, ConfigDataLocation.of("not-a-configserver:address"))).isFalse();

		verifyNoInteractions(context);
	}

	@Test
	void shouldNotRegisterAnyBeanWhenConfigIsNotEnabled() {
		environment.setProperty("spring.cloud.config.enabled", "false");
		when(context.getBinder()).thenReturn(binder);

		assertThat(resolver.isResolvable(context, ConfigDataLocation.of("optional:configserver:address"))).isFalse();
	}

	@Nested
	class EnabledCloudConfigTest {

		@BeforeEach
		void setup() {
			when(context.getBinder()).thenReturn(binder);
			when(context.getBootstrapContext()).thenReturn(bootstrapContext);
		}

		@Test
		void shouldRegisterRequestTemplateFactoryAndRestTemplate() {
			assertThat(resolver.isResolvable(context, ConfigDataLocation.of("optional:configserver:address")))
				.isFalse();

			assertThat(bootstrapContext.isRegistered(ConfigClientRequestTemplateFactory.class)).isTrue();
			assertThat(bootstrapContext.isRegistered(RestTemplate.class)).isTrue();

			var template = bootstrapContext.get(RestTemplate.class);
			assertThat(template.getInterceptors()).isEmpty();
		}

		@Test
		void shouldThrowExceptionWhenRequestReadTimeoutIsNegative() {
			environment.setProperty("spring.cloud.config.request-read-timeout", "-1");

			assertThatThrownBy(
					() -> resolver.isResolvable(context, ConfigDataLocation.of("optional:configserver:address")))
				.isInstanceOf(IllegalStateException.class);
		}

		@Test
		void shouldThrowExceptionWhenRequestConnectTimeoutIsNegative() {
			environment.setProperty("spring.cloud.config.request-connect-timeout", "-1");

			assertThatThrownBy(
					() -> resolver.isResolvable(context, ConfigDataLocation.of("optional:configserver:address")))
				.isInstanceOf(IllegalStateException.class);
		}

		@Test
		void shouldRegisterGenericRequestHeaderInterceptorIfAnyHeaderProvided() {
			environment.setProperty("spring.cloud.config.headers.foo", "bar");

			assertThat(resolver.isResolvable(context, ConfigDataLocation.of("optional:configserver:address")))
				.isFalse();

			assertThat(bootstrapContext.isRegistered(RestTemplate.class)).isTrue();

			var template = bootstrapContext.get(RestTemplate.class);
			assertThat(template.getInterceptors()).hasSize(1);

			var interceptor = template.getInterceptors().get(0);
			assertThat(interceptor)
				.isInstanceOf(ConfigClientRequestTemplateFactory.GenericRequestHeaderInterceptor.class);
		}

		@Test
		void shouldRemoveAuthorizationHeaderWhenRegisteringInterceptor() {
			environment.setProperty("spring.cloud.config.headers.authorization", "basic foo:bar");

			assertThat(resolver.isResolvable(context, ConfigDataLocation.of("optional:configserver:address")))
				.isFalse();

			assertThat(bootstrapContext.isRegistered(RestTemplate.class)).isTrue();

			var template = bootstrapContext.get(RestTemplate.class);
			assertThat(template.getInterceptors()).isEmpty();
		}

		@Test
		void shouldRegisterOAuth2InterceptorIfOauthPropertiesProvided() {
			environment.setProperty("spring.cloud.config.client.oauth2.client-id", "client-id");
			environment.setProperty("spring.cloud.config.client.oauth2.access-token-uri", "https://url");

			assertThat(resolver.isResolvable(context, ConfigDataLocation.of("optional:configserver:address")))
				.isFalse();

			assertThat(bootstrapContext.isRegistered(RestTemplate.class)).isTrue();

			var template = bootstrapContext.get(RestTemplate.class);
			assertThat(template.getInterceptors()).hasSize(1);

			var interceptor = template.getInterceptors().get(0);
			assertThat(interceptor).isInstanceOf(OAuth2AuthorizedClientHttpRequestInterceptor.class);
		}

		@Test
		void shouldOverridesPropertiesOfExistingRequestTemplateFactory() {
			environment.setProperty("spring.cloud.config.label", "default-label");
			environment.setProperty("spring.cloud.config.profile", "default-profile");

			assertThat(resolver.isResolvable(context, ConfigDataLocation.of("optional:configserver:address")))
				.isFalse();

			assertThat(bootstrapContext.isRegistered(ConfigClientRequestTemplateFactory.class)).isTrue();

			var factory = bootstrapContext.get(ConfigClientRequestTemplateFactory.class);
			assertThat(factory.getProperties().getLabel()).isEqualTo("default-label");
			assertThat(factory.getProperties().getProfile()).isEqualTo("default-profile");

			environment.setProperty("spring.cloud.config.label", "updated-label");
			environment.setProperty("spring.cloud.config.profile", "updated-profile");

			assertThat(resolver.isResolvable(context, ConfigDataLocation.of("optional:configserver:address")))
				.isFalse();

			assertThat(factory.getProperties().getLabel()).isEqualTo("updated-label");
			assertThat(factory.getProperties().getProfile()).isEqualTo("updated-profile");
		}

		@Test
		void shouldNotRegisterDuplicateInterceptorsWhenCalledMultipleTimes() {
			environment.setProperty("spring.cloud.config.headers.foo", "bar");
			environment.setProperty("spring.cloud.config.client.oauth2.client-id", "client-id");
			environment.setProperty("spring.cloud.config.client.oauth2.access-token-uri", "https://url");

			assertThat(resolver.isResolvable(context, ConfigDataLocation.of("optional:configserver:address")))
				.isFalse();

			assertThat(bootstrapContext.isRegistered(RestTemplate.class)).isTrue();

			var template = bootstrapContext.get(RestTemplate.class);
			assertThat(template.getInterceptors()).hasSize(2);

			assertThat(resolver.isResolvable(context, ConfigDataLocation.of("optional:configserver:address")))
				.isFalse();
			assertThat(template.getInterceptors()).hasSize(2);
		}

	}

}