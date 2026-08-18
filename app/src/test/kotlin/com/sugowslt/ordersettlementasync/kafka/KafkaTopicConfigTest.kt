package com.sugowslt.ordersettlementasync.kafka

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KafkaTopicConfigTest {

    private val config = KafkaTopicConfig(
        orderCreatedTopic = "order-created.v1",
        orderCreatedDltTopic = "order-created.v1-dlt",
        partitions = 3,
        replicas = 1,
    )

    @Test
    fun `원본 토픽과 DLT를 같은 파티션 수로 구성한다`() {
        val sourceTopic = config.orderCreatedTopic()
        val dltTopic = config.orderCreatedDltTopic()

        assertThat(sourceTopic.name()).isEqualTo("order-created.v1")
        assertThat(dltTopic.name()).isEqualTo("order-created.v1-dlt")
        assertThat(sourceTopic.numPartitions()).isEqualTo(3)
        assertThat(dltTopic.numPartitions()).isEqualTo(3)
        assertThat(sourceTopic.replicationFactor()).isEqualTo(1)
        assertThat(dltTopic.replicationFactor()).isEqualTo(1)
    }
}
