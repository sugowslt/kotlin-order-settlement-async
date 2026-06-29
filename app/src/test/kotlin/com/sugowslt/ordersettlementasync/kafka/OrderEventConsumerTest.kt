package com.sugowslt.ordersettlementasync.kafka

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant

class OrderEventConsumerTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `정상 payload는 forceFail가 false라 성공한다`() {
        val payload = mapper.writeValueAsString(
            OrderCreatedEvent(
                eventId = "evt-1",
                orderId = 1001L,
                occurredAt = Instant.now(),
            )
        )

        val contains = payload.contains("\"forceFail\":true")

        assertThat(contains).isFalse()
    }

    @Test
    fun `forceFail payload는 예외를 던진다`() {
        val payload = """{"eventId":"evt-2","orderId":1002L,"forceFail":true}"""

        assertThat(payload.contains("\"forceFail\":true")).isTrue()
    }

    @Test
    fun `JSON 파싱이 정상 동작한다`() {
        val event = OrderCreatedEvent(
            eventId = "evt-3",
            orderId = 1003L,
            occurredAt = Instant.parse("2026-01-02T03:04:05Z"),
        )

        val json = mapper.writeValueAsString(event)
        val parsed = mapper.readValue(json, OrderCreatedEvent::class.java)

        assertThat(parsed.eventId).isEqualTo("evt-3")
        assertThat(parsed.orderId).isEqualTo(1003L)
    }
}
