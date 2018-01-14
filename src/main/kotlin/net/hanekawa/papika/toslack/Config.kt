package net.hanekawa.papika.toslack

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

data class Config(
        val kafkaBootstrapServers: String,
        val toSlackTopic: String,
        val consumerGroup: String,
        val slackApiToken: String,
        val statsDHost: String,
        val statsDPort: Int
) {
    companion object Factory {
        fun load(): Config {
            val config = ConfigFactory.load()
            return Config(
                    kafkaBootstrapServers = config.extract<String>("kafkaBootstrapServers"),
                    toSlackTopic = config.extract<String>("toSlackTopic"),
                    consumerGroup = config.extract<String>("consumerGroup"),
                    slackApiToken = config.extract<String>("slackApiToken"),
                    statsDHost = config.extract<String>("statsDHost"),
                    statsDPort = config.extract<Int>("statsDPort")
            )
        }
    }
}
