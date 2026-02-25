package com.sugowslt.ordersettlementasync.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "infra")
data class InfraProperties(
    val redisHost: String,
    val redisPort: Int,
    val kafkaBootstrapServers: String,
    val consumerGroup: String,
)
