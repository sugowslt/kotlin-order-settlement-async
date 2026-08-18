package com.sugowslt.ordersettlementasync.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.ExponentialBackOff

@Configuration
class KafkaConfig(
    @Value("\${spring.kafka.listener.concurrency:2}") private val concurrency: Int,
    @Value("\${app.kafka.topics.order-created-dlt}") private val orderCreatedDltTopic: String,
    @Value("\${app.kafka.retry.initial-interval-ms:1000}") private val retryInitialIntervalMs: Long,
    @Value("\${app.kafka.retry.multiplier:2.0}") private val retryMultiplier: Double,
    @Value("\${app.kafka.retry.max-interval-ms:10000}") private val retryMaxIntervalMs: Long,
    @Value("\${app.kafka.retry.max-elapsed-time-ms:120000}") private val retryMaxElapsedTimeMs: Long,
) {

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>,
        kafkaErrorHandler: DefaultErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.setConsumerFactory(consumerFactory)
        factory.setConcurrency(concurrency)
        factory.setCommonErrorHandler(kafkaErrorHandler)
        return factory
    }

    @Bean
    fun kafkaErrorHandler(kafkaTemplate: KafkaTemplate<String, String>): DefaultErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
            deadLetterDestination(record)
        }
        val backOff = ExponentialBackOff()
        backOff.initialInterval = retryInitialIntervalMs
        backOff.multiplier = retryMultiplier
        backOff.maxInterval = retryMaxIntervalMs
        backOff.maxElapsedTime = retryMaxElapsedTimeMs

        return DefaultErrorHandler(recoverer, backOff).also {
            it.addNotRetryableExceptions(IllegalArgumentException::class.java)
            it.setAckAfterHandle(true)
        }
    }

    internal fun deadLetterDestination(record: ConsumerRecord<*, *>): TopicPartition =
        TopicPartition(orderCreatedDltTopic, record.partition())
}
