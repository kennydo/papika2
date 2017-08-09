package net.hanekawa.papika.common.slack

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

    private class Listener : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("onOpen: " + response)
        }

        @Synchronized override fun onMessage(webSocket: WebSocket?, text: String?) {
            println("onMessage: " + text!!)
        }

        override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
            webSocket!!.close(1000, null)
            println("onClose ($code): $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response) {
            println("onFailure " + response)
        }
    }

    fun run() {
        val rtmStartResponse = slackClient.getRtmStartResponse()
        slackClient.httpClient.newWebSocket(Request.Builder()
                .url(rtmStartResponse.url)
                .build(), Listener())
    }

}

