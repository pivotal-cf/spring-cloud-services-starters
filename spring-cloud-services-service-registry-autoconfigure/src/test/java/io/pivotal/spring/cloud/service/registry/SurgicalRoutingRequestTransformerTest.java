/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.pivotal.spring.cloud.service.registry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SurgicalRoutingRequestTransformerTest {

	private final SurgicalRoutingRequestTransformer transformer = new SurgicalRoutingRequestTransformer();

	@Mock
	private ServiceInstance instance;
	@Mock
	private HttpRequest request;

	@Before
	public void setUp() {
		HttpHeaders existingHeaders = new HttpHeaders();
		existingHeaders.add("foo", "bar");
		existingHeaders.add("foo", "baz");
		when(request.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(existingHeaders));
	}

	@Test
	public void headerIsSetWhenMetadataPresent() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("cfAppGuid", "123");
		metadata.put("cfInstanceIndex", "4");
		when(instance.getMetadata()).thenReturn(metadata);

		HttpRequest transformedRequest = transformer.transformRequest(request, instance);

		assertThat(transformedRequest.getHeaders().get("foo"), contains("bar", "baz"));
		assertEquals("123:4", transformedRequest.getHeaders().getFirst("X-CF-APP-INSTANCE"));

	}

	@Test
	public void headerIsNotSetWhenMetadataNotPresent() {
		when(instance.getMetadata()).thenReturn(Collections.emptyMap());

		HttpRequest transformedRequest = transformer.transformRequest(request, instance);

		assertThat(transformedRequest.getHeaders().get("foo"), contains("bar", "baz"));
		Assert.assertNull(transformedRequest.getHeaders().getFirst("X-CF-APP-INSTANCE"));

	}

}
