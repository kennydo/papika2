package net.hanekawa.papika.toslack

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import net.hanekawa.papika.common.getLogger
import net.hanekawa.papika.common.slack.SlackClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.io.IOException
import java.util.*


data class KafkaPayload(
        val channel: String?,
        val text: String?,
        @Json(name = "as_user") val asUser: String?,
        val attachments: List<Map<String, Any>>?,
        @Json(name = "icon_emoji") val iconEmoji: String?,
        @Json(name = "icon_url") val iconUrl: String?,
        @Json(name = "link_names") val linkNames: String?,
        @Json(name = "parse") val parse: String?,
        @Json(name = "reply_broadcast") val replyBroadcast: String?,
        @Json(name = "thread_ts") val threadTs: String?,
        @Json(name = "unfurl_links") val unfurlLinks: String?,
        @Json(name = "unfurl_media") val unfurlMedia: String?,
        val username: String?
)


class KafkaRecordHandler(val slackClient: SlackClient) {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    private val moshi = Moshi.Builder().build()
    private val listAdapter = moshi.adapter(List::class.java)
    private val kafkaPayloadAdapter = moshi.adapter(KafkaPayload::class.java)

    private fun isValidMessageForSlack(payload: KafkaPayload): Boolean {
        if (payload.channel == null) {
            LOG.info("Payload {} did not include key 'channel'", payload)
            return false
        }

        if (payload.text == null && payload.attachments == null) {
            LOG.info("Payload {} did not include one of either 'text' or 'attachments'", payload)
            return false
        }

        return true
    }

    fun onKafkaRecord(kafkaRecord: ConsumerRecord<String, String>) {
        val kafkaPayload = try {
            kafkaPayloadAdapter.fromJson(kafkaRecord.value())
        } catch (e: IOException) {
            PapikaToSlackBridge.LOG.info("Unable to parse record: {}", kafkaRecord.value())
            null
        } ?: return

        if (!isValidMessageForSlack(kafkaPayload)) {
            LOG.info("Not sending record because it does not meet Slack's API")
            return
        }

        val payload = HashMap<String, String?>()
        payload["channel"] = kafkaPayload.channel
        payload["text"] = kafkaPayload.text
        payload["as_user"] = kafkaPayload.asUser ?: "true"
        payload["attachments"] = listAdapter.toJson(kafkaPayload.attachments)
        payload["icon_emoji"] = kafkaPayload.iconEmoji
        payload["icon_url"] = kafkaPayload.iconUrl
        payload["link_names"] = kafkaPayload.linkNames ?: "true"
        payload["parse"] = kafkaPayload.parse ?: "none"
        payload["reply_broadcast"] = kafkaPayload.replyBroadcast ?: "false"
        payload["thread_ts"] = kafkaPayload.threadTs
        payload["unfurl_links"] = kafkaPayload.unfurlLinks ?: "false"
        payload["unfurl_media"] = kafkaPayload.unfurlMedia ?: "true"
        payload["username"] = kafkaPayload.username

        LOG.info("Posting message to Slack: {}", payload)
        slackClient.callApi("chat.postMessage", payload)
    }
}