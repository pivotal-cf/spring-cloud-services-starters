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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * Ensure client applications have `keys-to-sanitize` set so boot will automatically mask
 * sensitive properties. If client has manually set this property, merge it with any SCS
 * specific keys that need to be sanitized
 * <p>
 * Need to search for the composite source `configService:vault:...` or
 * `configService:credhub-` and add them to `keys-to-sanitize` because Boot doesn't
 * support recursively sanitizing all properties in a single source
 *
 * @author Ollie Hughes
 * @author Craig Walls
 * @author Dylan Roberts
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/6587"/>
 * @see <a href=
 * "https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html">
 * Spring Boot Common application properties</a>
 */
public class PropertyMaskingEnvironmentPostProcessor implements EnvironmentPostProcessor {

	static final String SANITIZE_ENV_KEY = "management.endpoint.env.keys-to-sanitize";

	private static final String VAULT_PROPERTY_PATTERN = "configserver:vault:";

	private static final String CREDHUB_PROPERTY_PATTERN = "configserver:credhub-";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String[] defaultKeys = { "password", "secret", "key", "token", ".*credentials.*", "vcap_services" };
		Set<String> propertiesToSanitize = Stream.of(defaultKeys).collect(Collectors.toSet());

		MutablePropertySources propertySources = environment.getPropertySources();
		Set<PropertySource<?>> configserverPropertySources = new HashSet<>();
		for (PropertySource<?> propertySource : propertySources) {
			if (propertySource.getName().startsWith("configserver:"))
				configserverPropertySources.add(propertySource);
		}

		Stream<String> vaultKeyNameStream = configserverPropertySources.stream()
			.filter(ps -> ps instanceof EnumerablePropertySource)
			.filter(ps -> ps.getName().startsWith(VAULT_PROPERTY_PATTERN)
					|| ps.getName().startsWith(CREDHUB_PROPERTY_PATTERN))
			.map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
			.flatMap(Arrays::stream);

		propertiesToSanitize.addAll(vaultKeyNameStream.collect(Collectors.toSet()));

		PropertiesPropertySource envKeysToSanitize = new PropertiesPropertySource(SANITIZE_ENV_KEY,
				mergeClientProperties(propertySources, propertiesToSanitize));

		environment.getPropertySources().addLast(envKeysToSanitize);

	}

	private Properties mergeClientProperties(MutablePropertySources propertySources, Set<String> propertiesToSanitize) {
		Properties props = new Properties();
		if (propertySources.contains(SANITIZE_ENV_KEY)) {
			String clientProperties = Objects.requireNonNull(propertySources.get(SANITIZE_ENV_KEY)).toString();
			propertiesToSanitize.addAll(Stream.of(clientProperties.split(",")).collect(Collectors.toSet()));
		}
		props.setProperty(SANITIZE_ENV_KEY, StringUtils.arrayToCommaDelimitedString(propertiesToSanitize.toArray()));
		return props;
	}

}
