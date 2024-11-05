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
package io.pivotal.spring.cloud.service.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * An EnvironmentPostProcessor to configure the load-balancer if feature flag is enabled.
 */
public class EurekaClientEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	static final String ZONE_CONFIGURATION_FLAG = "scs.starters.eureka.client.zone.configuration.enabled";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (isEnabled(environment)) {
			environment.getPropertySources()
				.addFirst(new MapPropertySource("EurekaClientLoadBalancerZonePreference",
						Map.of("spring.cloud.loadbalancer.configurations", "zone-preference")));
		}
	}

	/**
	 * It should be after {@link ConfigDataEnvironmentPostProcessor}, to make sure all
	 * other configurations are loaded.
	 * @return the order of this {@link EnvironmentPostProcessor}
	 */
	@Override
	public int getOrder() {
		return ConfigDataEnvironmentPostProcessor.ORDER + 1;
	}

	private boolean isEnabled(Environment environment) {
		return environment.getProperty(ZONE_CONFIGURATION_FLAG, Boolean.class, false);
	}

}
