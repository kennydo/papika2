package net.hanekawa.papika.fromslack

import com.squareup.moshi.Moshi
import net.hanekawa.papika.common.logging.getLogger
import net.hanekawa.papika.common.slack.RtmEvent
import net.hanekawa.papika.common.slack.RtmEventHandler
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord


class SlackEventHandler(val kafkaProducer: KafkaProducer<String, String>, val fromSlackTopic: String, val leadershipFlag: LeadershipFlag) : RtmEventHandler {
    companion object {
        val LOG = getLogger(this::class.java)

        // We do not want to forward events of these types to Kafka
        val blacklistedEventTypes = setOf("hello", "goodbye", "reconnect_url")
    }

    private val moshi = Moshi.Builder().build()
    private val mapAdapter = moshi.adapter(Map::class.java)

    override fun onEvent(event: RtmEvent) {
        if (blacklistedEventTypes.contains(event.type)) {
            LOG.debug("Ignoring blacklisted event of type {}: {}", event.type, event.payload)
            return
        }

        val eventJson = mapAdapter.toJson(event.payload)

        synchronized(leadershipFlag) {
            if (!leadershipFlag.isLeader) {
                LOG.info("Not producing event to Kafka because not leader: {}", eventJson)
                return
            }

            val record = ProducerRecord<String, String>(fromSlackTopic, eventJson)

            LOG.info("Producing event to Kafka: {}", eventJson)
            kafkaProducer.send(record)
        }
    }

}