package com.sugowslt.ordersettlementasync.kafka

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.concurrent.atomic.AtomicLong

@Component
class OrderEventConsumer(
    @Value("\${app.kafka.metrics.log-every:1000}") private val logEvery: Long,
) {

    private val logger = LoggerFactory.getLogger(OrderEventConsumer::class.java)
    private val objectMapper = jacksonObjectMapper()
    private val successCounter = AtomicLong(0)

    private fun payloadContainsForceFail(payload: String): Boolean {
        return payload.contains("\"forceFail\":true")
    }

    @KafkaListener(
        topics = ["\${app.kafka.topics.order-created}"],
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun consumeOrderCreated(
        payload: String,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?,
        @Header(name = "X-Trace-Id", required = false) traceId: String?,
    ) {
        val safeTraceId = traceId ?: "unknown"

        runCatching {
            if (payloadContainsForceFail(payload)) {
                throw IllegalStateException("forced consume failure")
            }

            val count = successCounter.incrementAndGet()
            if (count % logEvery == 0L) {
                logger.info(
                    "kafka.consume.success.sampled traceId={} count={} topic={} lastKey={}",
                    safeTraceId,
                    count,
                    topic,
                    key,
                )
            }
        }.onFailure {
            val event = runCatching { objectMapper.readValue(payload, OrderCreatedEvent::class.java) }.getOrNull()
            logger.error(
                "kafka.consume.failed traceId={} topic={} key={} eventId={} orderId={} reason={}",
                safeTraceId,
                topic,
                key,
                event?.eventId ?: "unknown",
                event?.orderId ?: -1,
                it.message,
                it,
            )
            throw it
        }
    }
}
