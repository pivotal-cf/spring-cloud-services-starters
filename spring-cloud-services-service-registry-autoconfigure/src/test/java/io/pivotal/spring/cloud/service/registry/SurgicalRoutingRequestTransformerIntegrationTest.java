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
package io.pivotal.spring.cloud.service.registry;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.web.client.RestClient;

import static io.pivotal.spring.cloud.service.registry.SurgicalRoutingRequestTransformer.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = SurgicalRoutingRequestTransformerIntegrationTest.TestConfig.class, properties = {
		"vcap.application.uris[0]=www.route.local", "eureka.client.serviceUrl.defaultZone=https://eureka-server" })
public class SurgicalRoutingRequestTransformerIntegrationTest {

	@Mock
	private HttpRequest originalRequest;

	@Mock
	private ClientHttpRequestExecution execution;

	@Mock
	private ServiceInstance instance;

	private final byte[] body = new byte[] {};

	@Autowired
	private LoadBalancerRequestFactory requestFactory;

	@Test
	public void headerIsSetWhenMetadataPresent() throws Exception {
		var metadata = Map.of(CF_APP_GUID, "::app-guid::", CF_INSTANCE_INDEX, "::instance-index::");
		when(instance.getMetadata()).thenReturn(metadata);

		var originalHeaders = new HttpHeaders();
		originalHeaders.addAll("Accept", List.of("text/plain", "application/json"));
		Mockito.when(originalRequest.getHeaders()).thenReturn(originalHeaders);

		requestFactory.createRequest(originalRequest, body, execution).apply(instance);

		var httpRequestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
		verify(execution).execute(httpRequestCaptor.capture(), eq(body));

		var transformedRequest = httpRequestCaptor.getValue();
		assertThat(transformedRequest.getHeaders().get("Accept")).contains("text/plain", "application/json");
		assertThat(transformedRequest.getHeaders().getFirst(SURGICAL_ROUTING_HEADER))
			.isEqualTo("::app-guid::" + ":" + "::instance-index::");
	}

	@SpringBootApplication
	@EnableDiscoveryClient
	static class TestConfig {

		@LoadBalanced
		@Bean
		public RestClient restClient() {
			return RestClient.create();
		}

	}

}
