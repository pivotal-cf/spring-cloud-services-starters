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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration class to configure a Eureka instance's settings based on the value of
 * <code>spring.cloud.services.registrationMethod</code>. "route" will register
 * vcap.application.uris[0] while "direct" will register using the container IP address
 * and PORT environment variable. The default behaviour is "route".<br>
 * <br>
 * Any defined eureka.instance.* property will override those set by this
 * auto-configuration.
 *
 * @author Chris Schaefer
 * @author Will Tran
 */
@Configuration
@ConditionalOnProperty("eureka.client.serviceUrl.defaultZone")
@ConditionalOnExpression("'${vcap.application.uris[0]:}'!='' || '${cf.instance.ip:}'!=''")
public class EurekaInstanceAutoConfiguration {

	private static final Logger LOGGER = Logger.getLogger(EurekaInstanceAutoConfiguration.class.getName());

	private static final String UNKNOWN_ZONE = "unknown";

	private static final String INDETERMINATE_EUREKA_ZONE_MESSAGE = "Eureka zone could not be determined from %s=\"%s\". Using \"%s\".";

	private static final String DEFAULT_ZONE_PROPERTY = "eureka.client.serviceUrl.defaultZone";

	private static final String ROUTE_REGISTRATION_METHOD = "route";

	private static final String DIRECT_REGISTRATION_METHOD = "direct";

	private static final String INSTANCE_ID = "instanceId";

	private static final String ZONE = "zone";

	@Value("${vcap.application.uris[0]:}")
	private String hostname;

	@Value("${eureka.instance.hostname:}")
	private String instanceHostname;

	@Value("${vcap.application.application_id:}")
	private String cfAppGuid;

	@Value("${cf.instance.index:}")
	private String cfInstanceIndex;

	@Value("${cf.instance.internal.ip:}")
	private String ip;

	@Value("${port:-1}")
	private int port;

	@Value("${vcap.application.instance_id:${random.value}}")
	private String instanceId;

	@Value("${spring.cloud.services.registrationMethod:route}")
	private String registrationMethod;

	@Value("${" + DEFAULT_ZONE_PROPERTY + ":}")
	private String zoneUri;

	@Bean
	public VirtualHostNamesBean getVirtualHostNames() {
		return new VirtualHostNamesBean();
	}

	@Bean
	public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
		if (!ObjectUtils.isEmpty(registrationMethod)) {
			LOGGER.info("Eureka registration method: " + registrationMethod);

			if (ROUTE_REGISTRATION_METHOD.equals(registrationMethod)) {
				return getRouteRegistration();
			}

			if (DIRECT_REGISTRATION_METHOD.equals(registrationMethod)) {
				return getDirectRegistration();
			}
		}

		return getDefaultRegistration();
	}

	@Bean
	@ConditionalOnMissingBean(SurgicalRoutingRequestTransformer.class)
	public SurgicalRoutingRequestTransformer surgicalRoutingLoadBalancerRequestTransformer() {
		return new SurgicalRoutingRequestTransformer();
	}

	private SanitizingEurekaInstanceConfigBean getRouteRegistration() {
		SanitizingEurekaInstanceConfigBean eurekaInstanceConfigBean = getDefaults();
		eurekaInstanceConfigBean.setSecurePortEnabled(true);
		eurekaInstanceConfigBean.setInstanceId(determineHostname() + ":" + instanceId);
		return eurekaInstanceConfigBean;
	}

	private SanitizingEurekaInstanceConfigBean getDirectRegistration() {
		SanitizingEurekaInstanceConfigBean eurekaInstanceConfigBean = getDefaults();
		eurekaInstanceConfigBean.setPreferIpAddress(true);
		eurekaInstanceConfigBean.setNonSecurePort(port);
		eurekaInstanceConfigBean.setInstanceId(ip + ":" + instanceId);
		return eurekaInstanceConfigBean;
	}

	private SanitizingEurekaInstanceConfigBean getDefaults() {
		InetUtilsProperties inetUtilsProperties = new InetUtilsProperties();
		inetUtilsProperties.setDefaultHostname(determineHostname());
		inetUtilsProperties.setDefaultIpAddress(ip);

		SanitizingEurekaInstanceConfigBean eurekaInstanceConfigBean = new SanitizingEurekaInstanceConfigBean(
				new InetUtils(inetUtilsProperties));
		eurekaInstanceConfigBean.setHostname(determineHostname());
		eurekaInstanceConfigBean.setIpAddress(ip);
		Map<String, String> metadataMap = eurekaInstanceConfigBean.getMetadataMap();
		metadataMap.put(SurgicalRoutingRequestTransformer.CF_APP_GUID, cfAppGuid);
		metadataMap.put(SurgicalRoutingRequestTransformer.CF_INSTANCE_INDEX, cfInstanceIndex);
		metadataMap.put(INSTANCE_ID, instanceId);
		metadataMap.put(ZONE, zoneFromUri(zoneUri));

		return eurekaInstanceConfigBean;
	}

	private static String zoneFromUri(String defaultZoneUri) {
		String hostname = null;
		try {
			hostname = new URI(defaultZoneUri).getHost();
		}
		catch (URISyntaxException e) {
			LOGGER.warning(String.format(INDETERMINATE_EUREKA_ZONE_MESSAGE + " %s", DEFAULT_ZONE_PROPERTY,
					defaultZoneUri, UNKNOWN_ZONE, e));
			return UNKNOWN_ZONE;
		}
		if (hostname == null || !hostname.contains(".")) {
			LOGGER.warning(String.format(INDETERMINATE_EUREKA_ZONE_MESSAGE, DEFAULT_ZONE_PROPERTY, defaultZoneUri,
					UNKNOWN_ZONE));
			return UNKNOWN_ZONE;
		}
		return hostname.substring(hostname.indexOf('.') + 1);
	}

	private SanitizingEurekaInstanceConfigBean getDefaultRegistration() {
		LOGGER.info("Eureka registration method not provided, defaulting to route");
		return getRouteRegistration();
	}

	private String determineHostname() {
		return StringUtils.hasText(instanceHostname) ? instanceHostname : hostname;
	}

}
