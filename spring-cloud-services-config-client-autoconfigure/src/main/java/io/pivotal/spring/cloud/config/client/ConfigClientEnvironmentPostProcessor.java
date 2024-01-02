/*
 * Copyright 2021-2024 the original author or authors.
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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import io.pivotal.cfenv.core.CfEnv;

/**
 * Using {@link CfEnv} directly here as we need to set the
 * <code>spring.config.import</code> property before the
 * {@link ConfigDataEnvironmentPostProcessor} runs java-cfenv-boot library's
 * EnvironmentPostProcessor runs after the ConfigDataEnvironmentPostProcessor,
 * intentionally
 *
 * @author Dylan Roberts
 */
public class ConfigClientEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final String PROPERTY_SOURCE_NAME = ConfigClientEnvironmentPostProcessor.class.getSimpleName();

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		var configServices = new CfEnv().findServicesByTag("configuration");
		if (configServices.size() != 1) {
			return;
		}

		var credentials = configServices.get(0).getCredentials();

		var map = new HashMap<String, Object>();
		map.put(ConfigClientOAuth2Properties.PREFIX + ".client-id", credentials.getString("client_id"));
		map.put(ConfigClientOAuth2Properties.PREFIX + ".client-secret", credentials.getString("client_secret"));
		map.put(ConfigClientOAuth2Properties.PREFIX + ".access-token-uri", credentials.getString("access_token_uri"));
		map.put(ConfigClientOAuth2Properties.PREFIX + ".scope", "");

		map.put("spring.config.import", "optional:configserver:" + credentials.getUri());
		map.put("spring.cloud.refresh.additional-property-sources-to-retain", PROPERTY_SOURCE_NAME);

		environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, map));
	}

	@Override
	public int getOrder() {
		return ConfigDataEnvironmentPostProcessor.ORDER - 1;
	}

}
