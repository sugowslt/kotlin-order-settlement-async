package com.sugowslt.ordersettlementasync.kafka

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicLong

const val TRACE_ID_HEADER = "X-Trace-Id"

@Component
class OrderEventProducer(
	private val kafkaTemplate: KafkaTemplate<String, String>,
	@Value("\${app.kafka.topics.order-created}") private val orderCreatedTopic: String,
	@Value("\${app.kafka.metrics.log-every:1000}") private val logEvery: Long,
) {

	private val logger = LoggerFactory.getLogger(OrderEventProducer::class.java)
	private val successCounter = AtomicLong(0)

	fun publish(event: OrderCreatedEvent) {
		val record = createRecord(orderCreatedTopic, event)
		kafkaTemplate.send(record)
			.whenComplete { result, throwable ->
				if (throwable != null) {
					logger.error(
						"kafka.publish.failed topic={} key={} eventId={} reason={}",
						orderCreatedTopic,
						record.key(),
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
						record.key(),
						result?.recordMetadata?.partition(),
						result?.recordMetadata?.offset(),
					)
				}
			}
	}

	companion object {
		private val objectMapper = jacksonObjectMapper()

		internal fun createRecord(topic: String, event: OrderCreatedEvent): ProducerRecord<String, String> {
			val record = ProducerRecord(topic, "order:${event.orderId}", objectMapper.writeValueAsString(event))
			record.headers().add(TRACE_ID_HEADER, event.traceId.toByteArray(StandardCharsets.UTF_8))
			return record
		}
	}
}
