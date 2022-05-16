package io.pivotal.spring.cloud.service.registry;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Paul Aly
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "vcap.application.uris[0]=www.route.local", "cf.instance.ip=1.2.3.4", "cf.instance.port=54321",
				"eureka.client.serviceUrl.defaultZone=https://eureka-123.west.my-cf.com/eureka/",
				"vcap.application.instance_id=instance-id", "spring.application.name=app-name_",
				"spring.cloud.services.registrationMethod=route", "eureka.client.enabled=true",
				"eureka.instance.hostname=www.route.other" })
public class EurekaAutoConfigMultipleRoutesIntegrationTest {

	@Autowired
	private EurekaInstanceConfigBean config;

	@Test
	public void eurekaConfigBean() {
		assertEquals("www.route.other:instance-id", config.getInstanceId());
		assertEquals("app-name-", config.getAppname());
		assertEquals("app-name-", config.getVirtualHostName());
		assertEquals("app-name-", config.getSecureVirtualHostName());
		assertEquals("www.route.other", config.getHostname());
		assertEquals(80, config.getNonSecurePort());
		assertEquals(443, config.getSecurePort());
		assertTrue(config.getSecurePortEnabled());
		assertEquals("west.my-cf.com", config.getMetadataMap().get("zone"));
	}

}
