package com.sugowslt.ordersettlementasync.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KafkaConfigTest {

    private val config = KafkaConfig(
        concurrency = 2,
        orderCreatedDltTopic = "order-created.v1-dlt",
        retryInitialIntervalMs = 1_000,
        retryMultiplier = 2.0,
        retryMaxIntervalMs = 10_000,
        retryMaxElapsedTimeMs = 120_000,
    )

    @Test
    fun `DLT는 원본 레코드의 파티션을 유지한다`() {
        val record = ConsumerRecord("order-created.v1", 3, 10L, "order:1001", "payload")

        val destination = config.deadLetterDestination(record)

        assertThat(destination.topic()).isEqualTo("order-created.v1-dlt")
        assertThat(destination.partition()).isEqualTo(3)
    }
}
