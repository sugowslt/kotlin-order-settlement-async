package com.sugowslt.ordersettlementasync.kafka

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.ExponentialBackOff
import java.time.Duration

@Configuration
class KafkaConfig(
    @Value("\${spring.kafka.listener.concurrency:2}") private val concurrency: Int,
) {

    @Bean
    fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, String>): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.setConsumerFactory(consumerFactory)
        factory.setConcurrency(concurrency)
        factory.setCommonErrorHandler(errorHandler())
        return factory
    }

    fun errorHandler(): CommonErrorHandler {
        val backOff = ExponentialBackOff()
        backOff.initialInterval = Duration.ofSeconds(1).toMillis()
        backOff.multiplier = 2.0
        backOff.maxInterval = Duration.ofSeconds(10).toMillis()
        backOff.maxElapsedTime = Duration.ofMinutes(2).toMillis()
        return DefaultErrorHandler(null, backOff).also { it.setAckAfterHandle(true) }
    }
}
