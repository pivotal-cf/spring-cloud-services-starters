/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.pivotal.spring.cloud.service.registry;

import java.util.Collection;
import java.util.Optional;

import com.netflix.discovery.DiscoveryClient.DiscoveryClientOptionalArgs;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;

/**
 * @author Dylan Roberts
 */
public class EurekaClientOAuth2AutoConfigurationTest {
    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String TOKEN_URI = "tokenUri";

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EurekaClientOAuth2AutoConfiguration.class));
    @Test
    public void discoveryClientOptionalArgs() {
        contextRunner
                .withPropertyValues(
                        "eureka.client.oauth2.client-id=" + CLIENT_ID,
                        "eureka.client.oauth2.client-secret=" + CLIENT_SECRET,
                        "eureka.client.oauth2.access-token-uri=" + TOKEN_URI
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DiscoveryClientOptionalArgs.class);
                    DiscoveryClientOptionalArgs discoveryClientOptionalArgs =
                            context.getBean(DiscoveryClientOptionalArgs.class);
                    @SuppressWarnings("unchecked")
                    Collection<ClientFilter> filters = (Collection<ClientFilter>) ReflectionTestUtils
                            .getField(discoveryClientOptionalArgs, "additionalFilters");
                    Optional<ClientFilter> clientFilterOptional =
                            filters == null ? Optional.empty() : filters.stream().findFirst();
                    assertThat(clientFilterOptional).isNotEmpty();
                    EurekaOAuth2ClientFilterAdapter eurekaOAuth2ClientFilterAdapter =
                            (EurekaOAuth2ClientFilterAdapter) clientFilterOptional.get();
                    ClientRegistration clientRegistration = (ClientRegistration) ReflectionTestUtils.getField(
                            eurekaOAuth2ClientFilterAdapter, "clientRegistration");
                    assertThat(clientRegistration).isNotNull();
                    assertThat(clientRegistration.getClientId()).isEqualTo(CLIENT_ID);
                    assertThat(clientRegistration.getClientSecret()).isEqualTo(CLIENT_SECRET);
                    assertThat(clientRegistration.getProviderDetails().getTokenUri()).isEqualTo(TOKEN_URI);
                    assertThat(clientRegistration.getAuthorizationGrantType()).isEqualTo(CLIENT_CREDENTIALS);
                });
    }

}