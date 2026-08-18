package com.sugowslt.ordersettlementasync.kafka

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

class OrderEventProducerTest {

    @Test
    fun `주문 이벤트 레코드에 주문 키와 추적 헤더를 담는다`() {
        val record = OrderEventProducer.createRecord("order-created.v1", orderCreatedEvent())

        assertThat(record.topic()).isEqualTo("order-created.v1")
        assertThat(record.key()).isEqualTo("order:1001")
        assertThat(record.headers().lastHeader(TRACE_ID_HEADER).value().toString(StandardCharsets.UTF_8))
            .isEqualTo("trace-1")
        assertThat(record.value()).contains("\"eventId\":\"evt-1\"")
    }

    private fun orderCreatedEvent() = OrderCreatedEvent(
        eventId = "evt-1",
        occurredAt = "2026-08-18T06:00:00Z",
        traceId = "trace-1",
        orderId = 1001L,
        userId = 2001L,
        amount = BigDecimal("15000.50"),
        currency = "KRW",
        idempotencyKey = "order-create-1001",
    )
}
