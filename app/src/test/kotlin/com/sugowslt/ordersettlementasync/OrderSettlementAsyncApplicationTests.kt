package com.sugowslt.ordersettlementasync

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
class OrderSettlementAsyncApplicationTests {

	@Autowired
	private lateinit var environment: Environment

	@Autowired
	private lateinit var kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry

	@Test
	fun contextLoads() {
		assertEquals("false", environment.getProperty("spring.kafka.listener.auto-startup"))
		assertTrue(kafkaListenerEndpointRegistry.listenerContainers.none { it.isRunning })
	}

}
