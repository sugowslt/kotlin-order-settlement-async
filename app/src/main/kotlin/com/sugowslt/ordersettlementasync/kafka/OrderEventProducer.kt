package com.sugowslt.ordersettlementasync.kafka

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.concurrent.atomic.AtomicLong

@Component
class OrderEventProducer(
	private val kafkaTemplate: KafkaTemplate<String, String>,
	@Value("\${app.kafka.topics.order-created}") private val orderCreatedTopic: String,
	@Value("\${app.kafka.metrics.log-every:1000}") private val logEvery: Long,
) {

	private val logger = LoggerFactory.getLogger(OrderEventProducer::class.java)
	private val objectMapper = jacksonObjectMapper()
	private val successCounter = AtomicLong(0)

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

				val count = successCounter.incrementAndGet()
				if (count % logEvery == 0L) {
					logger.info(
						"kafka.publish.success.sampled count={} topic={} lastKey={} partition={} offset={}",
						count,
						orderCreatedTopic,
						key,
						result?.recordMetadata?.partition(),
						result?.recordMetadata?.offset(),
					)
				}
			}
	}
}
