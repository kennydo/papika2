package net.hanekawa.papika.common.slack

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import net.hanekawa.papika.common.logging.getLogger
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
                .build()
        private val mapAdapter = moshi.adapter(Map::class.java)

        override fun onOpen(webSocket: WebSocket, response: Response) {
            LOG.debug("Opening websocket got response: {}", response)
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            val parsedMessage = try {
                mapAdapter.fromJson(text)
            } catch (e: JsonDataException) {
                LOG.error("Unable to parse: {}", text)
                null
            } ?: return

            LOG.info("Got event: {}", mapAdapter.toJson(parsedMessage))
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

