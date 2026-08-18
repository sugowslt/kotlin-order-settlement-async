package com.sugowslt.ordersettlementasync.kafka

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
@ConditionalOnProperty(
    prefix = "app.kafka.topics",
    name = ["auto-create"],
    havingValue = "true",
    matchIfMissing = true,
)
class KafkaTopicConfig(
    @Value("\${app.kafka.topics.order-created}") private val orderCreatedTopic: String,
    @Value("\${app.kafka.topics.order-created-dlt}") private val orderCreatedDltTopic: String,
    @Value("\${app.kafka.topics.partitions:3}") private val partitions: Int,
    @Value("\${app.kafka.topics.replicas:1}") private val replicas: Int,
) {

    init {
        require(partitions > 0) { "Kafka topic partitions must be positive" }
        require(replicas > 0) { "Kafka topic replicas must be positive" }
    }

    @Bean
    fun orderCreatedTopic(): NewTopic = topic(orderCreatedTopic)

    @Bean
    fun orderCreatedDltTopic(): NewTopic = topic(orderCreatedDltTopic)

    private fun topic(name: String): NewTopic = TopicBuilder.name(name)
        .partitions(partitions)
        .replicas(replicas)
        .build()
}
