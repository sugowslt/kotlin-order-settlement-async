package com.sugowslt.ordersettlementasync.kafka

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent
import com.sugowslt.ordersettlementasync.settlement.SettlementProcessResult
import com.sugowslt.ordersettlementasync.settlement.SettlementProcessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import kotlin.test.assertFailsWith

class OrderEventConsumerTest {
    private val mapper = jacksonObjectMapper()
    private val processedEvents = mutableListOf<OrderCreatedEvent>()
    private val processor = SettlementProcessor { event ->
        processedEvents += event
        SettlementProcessResult.CREATED
    }
    private val consumer = OrderEventConsumer(logEvery = 1_000, settlementProcessor = processor)

    @Test
    fun `정상 payload는 forceFail가 false라 성공한다`() {
        val payload = mapper.writeValueAsString(orderCreatedEvent(eventId = "evt-1", orderId = 1001L))

        consumer.consumeOrderCreated(
            payload = payload,
            topic = "order-created.v1",
            key = "order:1001",
            traceId = "trace-1",
        )

        assertThat(processedEvents.single().eventId).isEqualTo("evt-1")
    }

    @Test
    fun `forceFail payload는 예외를 던진다`() {
        val payload = mapper.writeValueAsString(
            orderCreatedEvent(eventId = "evt-2", orderId = 1002L, forceFail = true),
        )

        assertFailsWith<IllegalStateException> {
            consumer.consumeOrderCreated(
                payload = payload,
                topic = "order-created.v1",
                key = "order:1002",
                traceId = "trace-2",
            )
        }

        assertThat(processedEvents).isEmpty()
    }

    @Test
    fun `잘못된 JSON은 재시도하지 않는 이벤트 오류로 변환한다`() {
        assertFailsWith<InvalidOrderEventException> {
            consumer.consumeOrderCreated(
                payload = "{not-json}",
                topic = "order-created.v1",
                key = "order:1001",
                traceId = "trace-invalid",
            )
        }

        assertThat(processedEvents).isEmpty()
    }

    @Test
    fun `JSON 파싱이 정상 동작한다`() {
        val event = orderCreatedEvent(eventId = "evt-3", orderId = 1003L)

        val json = mapper.writeValueAsString(event)
        val parsed = mapper.readValue(json, OrderCreatedEvent::class.java)

        assertThat(parsed.eventId).isEqualTo("evt-3")
        assertThat(parsed.orderId).isEqualTo(1003L)
    }

    private fun orderCreatedEvent(
        eventId: String,
        orderId: Long,
        forceFail: Boolean = false,
    ) = OrderCreatedEvent(
        eventId = eventId,
        occurredAt = "2026-01-02T03:04:05Z",
        traceId = "trace-$orderId",
        orderId = orderId,
        userId = 2001L,
        amount = BigDecimal("15000.50"),
        currency = "KRW",
        idempotencyKey = "order-create-$orderId",
        forceFail = forceFail,
    )
}
