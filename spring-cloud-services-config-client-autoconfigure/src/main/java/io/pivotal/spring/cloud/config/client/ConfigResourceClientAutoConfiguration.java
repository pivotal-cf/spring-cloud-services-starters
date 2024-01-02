/*
 * Copyright 2017-2024 the original author or authors.
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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * @author Daniel Lavoie
 * @author Roy Clarkson
 * @author Dylan Roberts
 */
@AutoConfiguration(after = ConfigClientAutoConfiguration.class)
@ConditionalOnClass({ ConfigClientProperties.class })
public class ConfigResourceClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ConfigResourceClient.class)
	@ConditionalOnBean(value = RestTemplate.class, name = "configClientRestTemplate")
	public ConfigResourceClient configResourceClient(RestTemplate configClientRestTemplate,
			ConfigClientProperties configClientProperties) {
		return new DefaultConfigResourceClient(configClientRestTemplate, configClientProperties);
	}

}
