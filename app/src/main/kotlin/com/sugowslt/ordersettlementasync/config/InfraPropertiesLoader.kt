package com.sugowslt.ordersettlementasync.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
@EnableConfigurationProperties(InfraProperties::class)
class InfraPropertiesLoader(
    private val infraProperties: InfraProperties,
    private val environment: Environment,
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(InfraPropertiesLoader::class.java)

    override fun run(args: ApplicationArguments) {
        logger.info(
            "infra.config.loaded kafkaBootstrapServers={} consumerGroup={} serverPort={}",
            infraProperties.kafkaBootstrapServers,
            infraProperties.consumerGroup,
            environment.getProperty("server.port") ?: "8080",
        )
    }
}
