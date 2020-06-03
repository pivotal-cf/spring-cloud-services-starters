/*
 * Copyright 2019 the original author or authors.
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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.logging.Logger;

final class SanitizingEurekaInstanceConfigBean extends EurekaInstanceConfigBean implements InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(SanitizingEurekaInstanceConfigBean.class.getName());

	public SanitizingEurekaInstanceConfigBean(InetUtils inetUtils) {
		super(inetUtils);
	}

	@Override
	public void setEnvironment(Environment environment) {
		super.setEnvironment(environment);
		// set some defaults from the environment, but allow the defaults to use
		// relaxed binding
		String springAppName = getSpringApplicationName();
		String eurekaInstanceAppname = getEurekaInstanceAppnameProperty();
		if (StringUtils.hasText(eurekaInstanceAppname)) {
			// default to eureka.instance.appname if defined
			setVirtualHostName(eurekaInstanceAppname);
			setSecureVirtualHostName(eurekaInstanceAppname);
		} else if (StringUtils.hasText(springAppName)) {
			// default to a hostname-sanitized spring application name
			String sanitizedAppName = sanitizeHostname(springAppName);
			if (!springAppName.equals(sanitizedAppName)) {
				LOGGER.warning("Spring application name '" + springAppName
						+ "' was sanitized to produce eureka.instance.appname '" + sanitizedAppName + "'");
			}
			setAppname(sanitizedAppName);
			setVirtualHostName(sanitizedAppName);
			setSecureVirtualHostName(sanitizedAppName);
		}
	}

	private String getSpringApplicationName() {

		return Binder.get(getEnvironment()).bind("spring.application.name", String.class).orElse(null);
	}

	private String getEurekaInstanceAppnameProperty() {
		return Binder.get(getEnvironment()).bind("eureka.instance.appname", String.class).orElse(null);
	}

	// RFC 952 defines the valid character set for hostnames.
	private String sanitizeHostname(String hostname) {
		if (hostname == null) {
			return null;
		}
		return hostname.replaceAll("[^0-9a-zA-Z\\-\\.]", "-");
	}

	@Override
	public void afterPropertiesSet() {
		String messageSuffix = "' is set to a different value than eureka.instance.appname '" + getAppname()
				+ "', and is disallowed in Spring Cloud Services. Try only setting eureka.instance.appname."
				+ " Please refer to our documentation and reach out to us if you think you require different values.";
		if (StringUtils.hasText(getVirtualHostName()) && !getVirtualHostName().equalsIgnoreCase(getAppname())) {
			throw new IllegalArgumentException(
					"eureka.instance.virtualHostName '" + getVirtualHostName() + messageSuffix);
		}
		if (StringUtils.hasText(getSecureVirtualHostName()) && !getSecureVirtualHostName().equalsIgnoreCase(getAppname())) {
			throw new IllegalArgumentException(
					"eureka.instance.secureVirtualHostName '" + getSecureVirtualHostName() + messageSuffix);
		}
	}

}
