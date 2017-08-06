package net.hanekawa.papika.fromslack

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

data class Config(val kafkaBootstrapServers: String, val kafkaTopic: String, val slackApiToken: String) {
    companion object Factory {
        fun load(): Config {
            val config = ConfigFactory.load()
            return Config(
                    kafkaBootstrapServers = config.extract<String>("kafkaBootstrapServers"),
                    kafkaTopic = config.extract<String>("kafkaTopic"),
                    slackApiToken = config.extract<String>("slackApiToken")
            )
        }
    }
}
