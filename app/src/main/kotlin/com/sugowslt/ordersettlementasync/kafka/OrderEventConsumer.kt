package com.sugowslt.ordersettlementasync.kafka

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent
import com.sugowslt.ordersettlementasync.settlement.SettlementProcessResult
import com.sugowslt.ordersettlementasync.settlement.SettlementProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.concurrent.atomic.AtomicLong

@Component
@ConditionalOnProperty(
    prefix = "app.kafka.consumer",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class OrderEventConsumer(
    @Value("\${app.kafka.metrics.log-every:1000}") private val logEvery: Long,
    private val settlementProcessor: SettlementProcessor,
) {

    private val logger = LoggerFactory.getLogger(OrderEventConsumer::class.java)
    private val objectMapper = jacksonObjectMapper()
    private val successCounter = AtomicLong(0)

    @KafkaListener(
        topics = ["\${app.kafka.topics.order-created}"],
        containerFactory = "kafkaListenerContainerFactory",
        autoStartup = "\${spring.kafka.listener.auto-startup:true}",
    )
    fun consumeOrderCreated(
        payload: String,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?,
        @Header(name = TRACE_ID_HEADER, required = false) traceId: String?,
    ) {
        runCatching {
            val event = deserialize(payload)
            val resolvedTraceId = traceId ?: event.traceId
            if (event.forceFail) {
                throw IllegalStateException("forced consume failure")
            }

            val processResult = settlementProcessor.process(event)
            if (processResult == SettlementProcessResult.DUPLICATE) {
                logger.info(
                    "kafka.consume.duplicate traceId={} topic={} key={} eventId={} orderId={}",
                    resolvedTraceId,
                    topic,
                    key,
                    event.eventId,
                    event.orderId,
                )
                return@runCatching
            }

            val count = successCounter.incrementAndGet()
            if (count % logEvery == 0L) {
                logger.info(
                    "kafka.consume.success.sampled traceId={} count={} topic={} lastKey={}",
                    resolvedTraceId,
                    count,
                    topic,
                    key,
                )
            }
        }.onFailure {
            val event = runCatching { deserialize(payload) }.getOrNull()
            val resolvedTraceId = traceId ?: event?.traceId ?: "unknown"
            logger.error(
                "kafka.consume.failed traceId={} topic={} key={} eventId={} orderId={} reason={}",
                resolvedTraceId,
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

    private fun deserialize(payload: String): OrderCreatedEvent = try {
        objectMapper.readValue(payload, OrderCreatedEvent::class.java)
    } catch (exception: Exception) {
        throw InvalidOrderEventException("invalid order-created event payload", exception)
    }
}

class InvalidOrderEventException(message: String, cause: Throwable) : IllegalArgumentException(message, cause)
