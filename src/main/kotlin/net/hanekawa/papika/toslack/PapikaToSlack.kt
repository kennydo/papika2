package net.hanekawa.papika.toslack

import net.hanekawa.papika.common.getLogger
import net.hanekawa.papika.common.slack.SlackClient
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.*


class PapikaToSlackBridge(val config: Config) {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    fun run() {
        LOG.info("Starting the bridge from Kafka to Slack")

        val slackClient = SlackClient(config.slackApiToken)

        LOG.info("Connecting to Kafka servers {} with consumer group {}", config.kafkaBootstrapServers, config.consumerGroup)
        val kafkaConsumer = createKafkaConsumer(config.kafkaBootstrapServers, config.consumerGroup)

        LOG.info("Subscribing to topic: {}", config.toSlackTopic)
        kafkaConsumer.subscribe(arrayListOf(config.toSlackTopic))

        val kafkaHandler = KafkaRecordHandler(slackClient)

        while (true) {
            val records = kafkaConsumer.poll(500)
            records.forEach {
                try {
                    kafkaHandler.onKafkaRecord(it)
                } catch (e: Exception) {
                    LOG.error("Failed to handle Kafka record {}: {}", it, e)
                }
            }
        }
    }

    private fun createKafkaConsumer(bootstrapServers: String, consumerGroup: String): KafkaConsumer<String, String> {
        val props = Properties()
        props.put("bootstrap.servers", bootstrapServers)
        props.put("group.id", consumerGroup)
        props.put("enable.auto.commit", "true")
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        return KafkaConsumer(props)
    }
}


fun main(args: Array<String>) {
    val config = Config.load()
    val bridge = PapikaToSlackBridge(config)
    bridge.run()
}