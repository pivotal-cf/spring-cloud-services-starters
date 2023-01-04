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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

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

	private static final String SPRING_CLOUD_SERVICES_CONFIG_IMPORT = "springCloudServicesConfigImport";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		CfEnv cfEnv = new CfEnv();
		List<CfService> configServices = cfEnv.findServicesByTag("configuration");
		if (configServices.size() != 1)
			return;
		CfCredentials credentials = configServices.stream().findFirst().get().getCredentials();
		environment.getPropertySources().addFirst(oauth2PropertySource(credentials));
		environment.getPropertySources().addFirst(configImportPropertySource(credentials));
	}

	@Override
	public int getOrder() {
		return ConfigDataEnvironmentPostProcessor.ORDER - 1;
	}

	private MapPropertySource configImportPropertySource(CfCredentials credentials) {
		Map<String, Object> map = new HashMap<>();
		map.put("spring.config.import", "optional:configserver:" + credentials.getUri());
		map.put("spring.cloud.refresh.additional-property-sources-to-retain", SPRING_CLOUD_SERVICES_CONFIG_IMPORT);
		return new MapPropertySource(SPRING_CLOUD_SERVICES_CONFIG_IMPORT, map);
	}

	/**
	 * This method can be removed once java-cfenv supports config-client.
	 */
	private MapPropertySource oauth2PropertySource(CfCredentials credentials) {
		Map<String, Object> map = new HashMap<>();
		map.put("spring.cloud.config.uri", credentials.getUri());
		map.put("spring.cloud.config.client.oauth2.client-id", credentials.getString("client_id"));
		map.put("spring.cloud.config.client.oauth2.client-secret", credentials.getString("client_secret"));
		map.put("spring.cloud.config.client.oauth2.access-token-uri", credentials.getString("access_token_uri"));

		return new MapPropertySource("CfConfigClientProcessor", map);
	}

}
