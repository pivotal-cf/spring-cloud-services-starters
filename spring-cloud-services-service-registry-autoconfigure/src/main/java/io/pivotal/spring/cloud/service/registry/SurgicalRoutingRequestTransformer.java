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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestTransformer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;

/**
 * Adds a surgical routing header to the request if CF App GUID and CF Instance Index are
 * present in metadata.
 *
 * @see <a href=
 * 'https://docs.cloudfoundry.org/devguide/deploy-apps/routes-domains.html#surgical-routing'>
 * https://docs.cloudfoundry.org/devguide/deploy-apps/routes-domains.html#
 * surgical-routing</a>
 * @author William Tran
 */
public class SurgicalRoutingRequestTransformer implements LoadBalancerRequestTransformer {

	public static final String CF_APP_GUID = "cfAppGuid";

	public static final String CF_INSTANCE_INDEX = "cfInstanceIndex";

	public static final String SURGICAL_ROUTING_HEADER = "X-CF-APP-INSTANCE";

	@Override
	public HttpRequest transformRequest(HttpRequest request, ServiceInstance instance) {
		if (instance == null) {
			return request;
		}

		var metadata = instance.getMetadata();
		if (!metadata.containsKey(CF_APP_GUID) || !metadata.containsKey(CF_INSTANCE_INDEX)) {
			return request;
		}

		final var headerValue = String.format("%s:%s", metadata.get(CF_APP_GUID), metadata.get(CF_INSTANCE_INDEX));

		// request.getHeaders might be immutable, so return a wrapper
		return new HttpRequestWrapper(request) {
			@Override
			public HttpHeaders getHeaders() {
				var headers = new HttpHeaders();
				headers.putAll(super.getHeaders());
				headers.add(SURGICAL_ROUTING_HEADER, headerValue);
				return headers;
			}
		};
	}

}
