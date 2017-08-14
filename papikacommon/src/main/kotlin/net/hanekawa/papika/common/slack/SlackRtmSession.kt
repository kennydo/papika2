package net.hanekawa.papika.common.slack

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import net.hanekawa.papika.common.logging.getLogger
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener


interface RtmEventHandler {
    fun onEvent(event: RtmEvent)
}


class JsonParsingListener(val messageHandler: RtmEventHandler) : WebSocketListener() {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    private val moshi = Moshi
            .Builder()
            .build()
    private val mapAdapter = moshi.adapter(Map::class.java)

    override fun onOpen(webSocket: WebSocket, response: Response) {
        SlackRtmSession.LOG.debug("Opening websocket got response: {}", response)
    }

    override fun onMessage(webSocket: WebSocket?, text: String?) {
        val parsedMessage = try {
            mapAdapter.fromJson(text)
        } catch (e: JsonDataException) {
            SlackRtmSession.LOG.error("Unable to parse: {}", text)
            null
        } ?: return

        SlackRtmSession.LOG.debug("Got event: {}", mapAdapter.toJson(parsedMessage))
        @Suppress("UNCHECKED_CAST")
        val castedEvent = parsedMessage as? Map<String, Any> ?: return

        val event = RtmEvent(
                type = castedEvent["type"] as String,
                payload = castedEvent
        )

        messageHandler.onEvent(event)
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        SlackRtmSession.LOG.info("Closing with code {}: {}", code, reason)
        webSocket!!.close(1000, null)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        SlackRtmSession.LOG.error("Unexpected failure: {}", response, t)
    }
}


class SlackRtmSession(val slackClient: SlackClient, val eventHandler: RtmEventHandler) {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    fun run() {
        val rtmStartResponse = slackClient.getRtmStartResponse()
        slackClient.httpClient.newWebSocket(Request.Builder()
                .url(rtmStartResponse.url)
                .build(), JsonParsingListener(eventHandler))
    }

}

