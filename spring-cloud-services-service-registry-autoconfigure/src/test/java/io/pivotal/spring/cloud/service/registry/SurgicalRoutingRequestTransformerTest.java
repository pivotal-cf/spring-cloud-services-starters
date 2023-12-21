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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

import static io.pivotal.spring.cloud.service.registry.SurgicalRoutingRequestTransformer.CF_APP_GUID;
import static io.pivotal.spring.cloud.service.registry.SurgicalRoutingRequestTransformer.CF_INSTANCE_INDEX;
import static io.pivotal.spring.cloud.service.registry.SurgicalRoutingRequestTransformer.SURGICAL_ROUTING_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SurgicalRoutingRequestTransformerTest {

	private final SurgicalRoutingRequestTransformer transformer = new SurgicalRoutingRequestTransformer();

	@Mock
	private ServiceInstance instance;

	@Mock
	private HttpRequest request;

	@BeforeEach
	public void setup() {
		var existingHeaders = new HttpHeaders();
		existingHeaders.addAll("Accept", List.of("text/plain", "text/html"));

		when(request.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(existingHeaders));
	}

	@Test
	public void headerIsSetWhenMetadataPresent() {
		var metadata = Map.of(CF_APP_GUID, "::guid::", CF_INSTANCE_INDEX, "::index::");
		when(instance.getMetadata()).thenReturn(metadata);

		var transformedRequest = transformer.transformRequest(request, instance);

		assertThat(transformedRequest.getHeaders().get("Accept")).contains("text/plain", "text/html");
		assertThat(transformedRequest.getHeaders().getFirst(SURGICAL_ROUTING_HEADER))
			.isEqualTo("::guid::" + ":" + "::index::");
	}

	@Test
	public void headerIsNotSetWhenAppGuidNotPresentInMetadata() {
		var metadata = Map.of(CF_INSTANCE_INDEX, "::index::");
		when(instance.getMetadata()).thenReturn(metadata);

		var transformedRequest = transformer.transformRequest(request, instance);

		assertThat(transformedRequest.getHeaders().get("Accept")).contains("text/plain", "text/html");
		assertThat(transformedRequest.getHeaders().getFirst(SURGICAL_ROUTING_HEADER)).isNull();
	}

	@Test
	public void headerIsNotSetWhenInstanceIndexPresentInMetadata() {
		var metadata = Map.of(CF_APP_GUID, "::guid::");
		when(instance.getMetadata()).thenReturn(metadata);

		var transformedRequest = transformer.transformRequest(request, instance);

		assertThat(transformedRequest.getHeaders().get("Accept")).contains("text/plain", "text/html");
		assertThat(transformedRequest.getHeaders().getFirst(SURGICAL_ROUTING_HEADER)).isNull();
	}

	@Test
	public void headerIsNotSetWhenServiceInstanceIsNull() {

		var transformedRequest = transformer.transformRequest(request, null);

		assertThat(transformedRequest.getHeaders().get("Accept")).contains("text/plain", "text/html");
		assertThat(transformedRequest.getHeaders().getFirst(SURGICAL_ROUTING_HEADER)).isNull();
	}

}
