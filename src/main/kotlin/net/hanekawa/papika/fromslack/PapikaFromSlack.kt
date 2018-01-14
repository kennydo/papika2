package net.hanekawa.papika.fromslack

import net.hanekawa.papika.common.getLogger
import net.hanekawa.papika.common.slack.SlackClient
import org.apache.kafka.clients.producer.KafkaProducer
import java.util.*


class PapikaFromSlackBridge(val config: Config) {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    fun run() {
        LOG.info("Starting the bridge from slack to kafka")

        LOG.info("Connecting to Kafka: {}", config.kafkaBootstrapServers)
        LOG.info("Sending Slack events to topic: {}", config.fromSlackTopic)
        val kafkaProducer = createKafkaProducer(config.kafkaBootstrapServers)

        LOG.info("Connecting to Zookeeper: {}", config.zookeeperConnect)
        val leaderCandidate = LeaderCandidate(config.zookeeperConnect)
        leaderCandidate.run()

        val eventHandler = SlackEventHandler(kafkaProducer, config.fromSlackTopic, leaderCandidate)

        LOG.info("Connecting to Slack")
        val slackClient = SlackClient(config.slackApiToken)

        slackClient.buildRtmSession(eventHandler).run()
    }

    private fun createKafkaProducer(bootstrapServers: String): KafkaProducer<String, String> {
        val props = Properties()
        props.put("bootstrap.servers", bootstrapServers)
        props.put("acks", "all")
        props.put("retries", 5)
        props.put("linger.ms", 0)
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

        val producer = KafkaProducer<String, String>(props)
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                LOG.info("Closing Kafka producer")
                producer.close()
            }
        })
        return producer
    }
}


fun main(args: Array<String>) {
    val config = Config.load()
    val bridge = PapikaFromSlackBridge(config)
    bridge.run()
}
