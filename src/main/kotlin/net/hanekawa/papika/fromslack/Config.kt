package net.hanekawa.papika.fromslack

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

data class Config(
        val kafkaBootstrapServers: String,
        val fromSlackTopic: String,
        val slackApiToken: String,
        val zookeeperConnect: String,
        val statsDHost: String,
        val statsDPort: Int
) {
    companion object Factory {
        fun load(): Config {
            val config = ConfigFactory.load()
            return Config(
                    kafkaBootstrapServers = config.extract<String>("kafkaBootstrapServers"),
                    fromSlackTopic = config.extract<String>("fromSlackTopic"),
                    slackApiToken = config.extract<String>("slackApiToken"),
                    zookeeperConnect = config.extract<String>("zookeeperConnect"),
                    statsDHost = config.extract<String>("statsDHost"),
                    statsDPort = config.extract<Int>("statsDPort")
            )
        }
    }
}
