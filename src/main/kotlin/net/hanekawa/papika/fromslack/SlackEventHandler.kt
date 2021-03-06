package net.hanekawa.papika.fromslack

import com.squareup.moshi.Json
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Rfc3339DateJsonAdapter
import com.timgroup.statsd.StatsDClient
import net.hanekawa.papika.common.getLogger
import net.hanekawa.papika.common.slack.RtmEvent
import net.hanekawa.papika.common.slack.RtmEventHandler
import okhttp3.Response
import okhttp3.WebSocket
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


class SlackEventHandler(
        private val statsd: StatsDClient,
        private val kafkaProducer: KafkaProducer<String, String>,
        private val fromSlackTopic: String,
        private val leaderCandidate: LeaderCandidate
) : RtmEventHandler {
    companion object {
        val LOG = getLogger(this::class.java)

        // We do not want to forward events of these types to Kafka
        val blacklistedEventTypes = setOf("hello", "goodbye", "reconnect_url")
    }

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).add(Date::class.java, Rfc3339DateJsonAdapter()).build()
    private val eventAdapter = moshi.adapter(FromSlackEvent::class.java)

    override fun onEvent(webSocket: WebSocket, event: RtmEvent) {
        val statsdTags = arrayOf("event_type:${event.type}")
        statsd.increment("handler.num_events_received", *statsdTags)

        if (blacklistedEventTypes.contains(event.type)) {
            LOG.debug("Ignoring blacklisted event of type {}: {}", event.type, event.payload)
            return
        }

        val kafkaEvent = FromSlackEvent(
                eventHeader = KafkaEventHeader.create(),
                event = event.payload
        )
        val eventJson = eventAdapter.toJson(kafkaEvent)

        synchronized(leaderCandidate) {
            if (!leaderCandidate.hasLeadership()) {
                statsd.increment("handler.num_events_discarded_as_non_leader", *statsdTags)
                LOG.info("Not producing event to Kafka because not leader: {}", eventJson)
                return
            }
        }

        val record = ProducerRecord<String, String>(fromSlackTopic, eventJson)

        LOG.info("Producing event to Kafka: {}", eventJson)
        statsd.increment("handler.num_events_sent_to_kafka", *statsdTags)
        kafkaProducer.send(record)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        LOG.warn("Relinquishing leadership due to web socket failure", t)
        statsd.increment("handler.num_failures")
        internalClose()
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        LOG.warn("Websocket closing code {}: {}", code, reason)
        statsd.increment("handler.num_closing")
        internalClose()
    }

    private fun internalClose() {
        leaderCandidate.abandonLeadership()

        // Block until all Kafka records are sent
        kafkaProducer.close()

    }
}