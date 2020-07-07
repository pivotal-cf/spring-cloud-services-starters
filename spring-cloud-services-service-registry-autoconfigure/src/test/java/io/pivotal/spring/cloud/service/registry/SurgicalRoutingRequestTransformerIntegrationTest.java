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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
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
	private LoadBalancerRequestFactory lbReqFactory;

	@Test
	public void transformer() throws Exception {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("cfAppGuid", "123");
		metadata.put("cfInstanceIndex", "4");
		HttpHeaders originalHeaders = new HttpHeaders();
		originalHeaders.add("foo", "bar");
		originalHeaders.add("foo", "baz");
		Mockito.when(instance.getMetadata()).thenReturn(metadata);
		Mockito.when(originalRequest.getHeaders()).thenReturn(originalHeaders);

		lbReqFactory.createRequest(originalRequest, body, execution).apply(instance);

		ArgumentCaptor<HttpRequest> httpRequestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
		verify(execution).execute(httpRequestCaptor.capture(), eq(body));
		HttpRequest transformedRequest = httpRequestCaptor.getValue();
		assertThat(transformedRequest.getHeaders().get("foo"), contains("bar", "baz"));
		assertEquals("123:4", transformedRequest.getHeaders().getFirst("X-CF-APP-INSTANCE"));
	}

	@SpringBootApplication
	@EnableDiscoveryClient
	static class TestConfig {

		@LoadBalanced
		@Bean
		public RestTemplate restTemplate() {
			return new RestTemplate();
		}

	}

}
