package net.hanekawa.papika.fromslack

import com.squareup.moshi.Json
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Rfc3339DateJsonAdapter
import net.hanekawa.papika.common.getLogger
import net.hanekawa.papika.common.slack.RtmEvent
import net.hanekawa.papika.common.slack.RtmEventHandler
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.net.InetAddress
import java.util.*


data class KafkaEventHeader(
        val timestamp: Date,
        val fqdn: String
) {
    companion object {
        fun create(): KafkaEventHeader {
            return KafkaEventHeader(
                    timestamp = Date(),
                    fqdn = InetAddress.getLocalHost().hostName
            )
        }
    }
}

data class FromSlackEvent(
        @Json(name = "event_header") val eventHeader: KafkaEventHeader,
        val event: Map<String, Any>
)


class SlackEventHandler(val kafkaProducer: KafkaProducer<String, String>, val fromSlackTopic: String, val leadershipFlag: LeadershipFlag) : RtmEventHandler {
    companion object {
        val LOG = getLogger(this::class.java)

        // We do not want to forward events of these types to Kafka
        val blacklistedEventTypes = setOf("hello", "goodbye", "reconnect_url")
    }

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).add(Date::class.java, Rfc3339DateJsonAdapter()).build()
    private val eventAdapter = moshi.adapter(FromSlackEvent::class.java)

    override fun onEvent(event: RtmEvent) {
        if (blacklistedEventTypes.contains(event.type)) {
            LOG.debug("Ignoring blacklisted event of type {}: {}", event.type, event.payload)
            return
        }

        val kafkaEvent = FromSlackEvent(
                eventHeader = KafkaEventHeader.create(),
                event = event.payload
        )
        val eventJson = eventAdapter.toJson(kafkaEvent)

        synchronized(leadershipFlag) {
            if (!leadershipFlag.isLeader) {
                LOG.info("Not producing event to Kafka because not leader: {}", eventJson)
                return
            }
        }

        val record = ProducerRecord<String, String>(fromSlackTopic, eventJson)

        LOG.info("Producing event to Kafka: {}", eventJson)
        kafkaProducer.send(record)
    }

}