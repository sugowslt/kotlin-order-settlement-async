package com.sugowslt.ordersettlementasync.kafka

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class OrderEventProducer(
	private val kafkaTemplate: KafkaTemplate<String, String>,
	@Value("\${app.kafka.topics.order-created}") private val orderCreatedTopic: String,
) {

	private val logger = LoggerFactory.getLogger(OrderEventProducer::class.java)
	private val objectMapper = jacksonObjectMapper()

	fun publish(event: OrderCreatedEvent) {
		val key = "order:${event.orderId}"
		val payload = objectMapper.writeValueAsString(event)
		kafkaTemplate.send(orderCreatedTopic, key, payload)
			.whenComplete { result, throwable ->
				if (throwable != null) {
					logger.error(
						"kafka.publish.failed topic={} key={} eventId={} reason={}",
						orderCreatedTopic,
						key,
						event.eventId,
						throwable.message,
						throwable,
					)
					return@whenComplete
				}

				logger.info(
					"kafka.publish.success topic={} key={} eventId={} partition={} offset={}",
					orderCreatedTopic,
					key,
					event.eventId,
					result?.recordMetadata?.partition(),
					result?.recordMetadata?.offset(),
				)
			}
	}
}
