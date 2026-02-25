package com.sugowslt.ordersettlementasync.kafka

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class OrderEventConsumer {

	private val logger = LoggerFactory.getLogger(OrderEventConsumer::class.java)
	private val objectMapper = jacksonObjectMapper()

	@KafkaListener(topics = ["\${app.kafka.topics.order-created}"])
	fun consumeOrderCreated(
		payload: String,
		@Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
		@Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?,
	) {
		runCatching {
			val event = objectMapper.readValue(payload, OrderCreatedEvent::class.java)

			if (event.forceFail) {
				throw IllegalStateException("forced consume failure")
			}

			logger.info(
				"kafka.consume.success topic={} key={} eventId={} orderId={} traceId={}",
				topic,
				key,
				event.eventId,
				event.orderId,
				event.traceId,
			)
		}.onFailure {
			logger.error(
				"kafka.consume.failed topic={} key={} payload={} reason={}",
				topic,
				key,
				payload,
				it.message,
				it,
			)
		}
	}
}
