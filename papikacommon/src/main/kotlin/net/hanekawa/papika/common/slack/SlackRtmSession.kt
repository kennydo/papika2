package net.hanekawa.papika.common.slack

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import net.hanekawa.papika.common.logging.getLogger
import net.hanekawa.papika.common.slack.eventmodels.MemberJoinedChannelEvent
import net.hanekawa.papika.common.slack.eventmodels.MemberLeftChannelEvent
import net.hanekawa.papika.common.slack.eventmodels.UserTypingEvent
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener


interface RtmMessageHandler {
    fun onMessage(content: String)
}


class SlackRtmSessionBuilder(val slackClient: SlackClient) {
    val messageHandler: RtmMessageHandler? = null

    fun build(): SlackRtmSession {
        return SlackRtmSession(slackClient = slackClient, messageHandler = messageHandler)
    }
}


class SlackRtmSession(val slackClient: SlackClient, val messageHandler: RtmMessageHandler?) {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    private class JsonParsingListener : WebSocketListener() {
        private val moshi = Moshi
                .Builder()
                .add(object {
                    @ToJson fun toJson(eventType: EventType): String {
                        return eventType.eventName
                    }

                    @FromJson fun fromJson(json: String): EventType? {
                        return EventType.fromEventName(json)
                    }
                })
                .add(object {
                    @ToJson fun toJson(channelType: ChannelType): String {
                        return channelType.channelType
                    }

                    @FromJson fun fromJson(json: String): ChannelType? {
                        return ChannelType.fromChannelType(json)
                    }
                })
                .build()
        private val mapAdapter = moshi.adapter(Map::class.java)

        override fun onOpen(webSocket: WebSocket, response: Response) {
            LOG.debug("Opening websocket got response: {}", response)
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            val maybeParsedMessage = try {
                mapAdapter.fromJson(text)
            } catch (e: JsonDataException) {
                LOG.error("Unable to parse: {}", text)
                null
            } ?: return

            val parsedMessage = maybeParsedMessage!!

            val parsedType = parsedMessage["type"] as? String
            val maybeEventType = EventType.fromEventName(parsedType)

            val maybeEventModelClass = when (maybeEventType) {
                EventType.MEMBER_JOINED_CHANNEL -> MemberJoinedChannelEvent::class.java
                EventType.MEMBER_LEFT_CHANNEL -> MemberLeftChannelEvent::class.java
                EventType.USER_TYPING -> UserTypingEvent::class.java
                else -> null
            }

            if (maybeEventModelClass == null) {
                LOG.info("Got unknown event of type {}: {}", parsedType, parsedMessage)
                return
            }

            val eventModelClass = maybeEventModelClass!!
            val eventType = maybeEventType!!

            val event = try {
                moshi.adapter(eventModelClass).fromJson(text)
            } catch (e: JsonDataException) {
                LOG.error("Unable to parse {} type event: {}", eventType.name, parsedMessage)
                null
            } ?: return

            LOG.info("Got event: {}", event.toString())
        }

        override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
            LOG.info("Closing with code {}: {}", code, reason)
            webSocket!!.close(1000, null)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            LOG.error("Unexpected failure: {}", response, t)
        }
    }

    fun run() {
        val rtmStartResponse = slackClient.getRtmStartResponse()
        slackClient.httpClient.newWebSocket(Request.Builder()
                .url(rtmStartResponse.url)
                .build(), JsonParsingListener())
    }

}

