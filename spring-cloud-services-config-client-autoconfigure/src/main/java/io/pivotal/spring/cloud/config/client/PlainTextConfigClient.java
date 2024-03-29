/*
 * Copyright 2019-2024 the original author or authors.
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

import org.springframework.core.io.Resource;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Provides access to plain text configuration files served by a Spring Cloud Config
 * Server.
 *
 * @author Daniel Lavoie
 */
public interface PlainTextConfigClient {

	/**
	 * Retrieves a plain text config file using the defaults profiles and labels.
	 * @param path config file path relative to spring application folder
	 * @return plain text file retrieved from config server
	 * @throws IllegalArgumentException when application name or Config Server url is
	 * undefined.
	 * @throws HttpClientErrorException when a config file is not found.
	 */
	Resource getPlainTextResource(String path);

	/**
	 * Retrieves a plain text config file.
	 * @throws IllegalArgumentException when application name or Config Server url is
	 * undefined.
	 * @throws HttpClientErrorException when a config file is not found.
	 */
	Resource getPlainTextResource(String profile, String label, String path);

}
