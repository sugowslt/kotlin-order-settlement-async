package com.sugowslt.ordersettlementasync.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "infra")
data class InfraProperties(
    val kafkaBootstrapServers: String,
    val consumerGroup: String,
)
