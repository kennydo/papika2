package net.hanekawa.papika.common.slack

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import net.hanekawa.papika.common.getLogger
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener


interface RtmEventHandler {
    fun onEvent(webSocket: WebSocket, event: RtmEvent)
    fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?)
    fun onClosing(webSocket: WebSocket?, code: Int, reason: String?)
}

data class PingEvent(
        val type: String = "ping",
        val id: String,
        val time: String
)


class JsonParsingListener(val messageHandler: RtmEventHandler) : WebSocketListener() {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    private val moshi = Moshi
            .Builder()
            .build()
    private val mapAdapter = moshi.adapter(Map::class.java)
    private var healthChecker: WebSocketHealthCheckingThread? = null

    override fun onOpen(webSocket: WebSocket, response: Response) {
        SlackRtmSession.LOG.debug("Opening websocket got response: {}", response)

        healthChecker = WebSocketHealthCheckingThread(webSocket)
        healthChecker!!.start()
    }

    override fun onMessage(webSocket: WebSocket, text: String?) {
        if (text == null) {
            SlackRtmSession.LOG.warn("Got null message: {}", text)
            return
        }

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

        if (event.type == "pong") {
            healthChecker!!.handlePong(event)
        } else {
            // Never expose ping/pongs to the actual message handler
            messageHandler.onEvent(webSocket, event)
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String?) {
        SlackRtmSession.LOG.info("Closing with code {}: {}", code, reason)
        healthChecker!!.shouldRun = false

        messageHandler.onClosing(webSocket, code, reason)

        // onClosing means the remote peer wants to close. We have to close our side too
        webSocket.close(1000, null)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        SlackRtmSession.LOG.error("Unexpected failure: {}", response, t)
        healthChecker!!.shouldRun = false

        messageHandler.onFailure(webSocket, t, response)
    }
}


class WebSocketHealthCheckingThread(private val webSocket: WebSocket, private val pingIntervalMs: Long = 5_000, private val unansweredPingsThreshold: Int = 5) : Thread() {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    private val moshi = Moshi
            .Builder()
            .build()
    private val pingEventAdapter = moshi.adapter(PingEvent::class.java)

    var shouldRun = true
    private var mostRecentPongReceivedAt: Long? = null

    override fun run() {
        var unansweredPingsCounter = 0

        while (shouldRun) {
            val mostRecentPingSentAt = System.currentTimeMillis()

            val pingEvent = PingEvent(
                    id = mostRecentPingSentAt.toString(),
                    time = mostRecentPingSentAt.toString()
            )

            val json = pingEventAdapter.toJson(pingEvent)

            LOG.debug("Sending ping: {}", json)
            webSocket.send(json)

            Thread.sleep(pingIntervalMs)

            // Assess health
            val mostRecentPong = synchronized(this) { mostRecentPongReceivedAt }

            when {
                mostRecentPong == null -> {
                    LOG.warn("No pong received")
                    // We have not received a pong at all
                    unansweredPingsCounter += 1
                }
                mostRecentPong < mostRecentPingSentAt -> {
                    LOG.warn("Pong was stale (ping={}, pong={}, diff={})", mostRecentPingSentAt, mostRecentPong, mostRecentPong - mostRecentPingSentAt)
                    // We have not received a pong since our latest ping
                    unansweredPingsCounter += 1
                }
                else -> {
                    unansweredPingsCounter = 0
                }
            }

            when {
                unansweredPingsCounter >= unansweredPingsThreshold -> {
                    LOG.info("Reached maximum threshold of unanswered pings ({}), declaring unhealthy", unansweredPingsThreshold)
                    onUnhealthy()
                }
                unansweredPingsCounter > 0 -> {
                    LOG.info("Unanswered pings: {} / {}", unansweredPingsCounter, unansweredPingsThreshold)
                }
            }

        }
        LOG.info("Healthchecker should no longer run, stopping")
    }

    fun handlePong(event: RtmEvent) {
        val pingTimeMs = event.payload["time"].toString().toLongOrNull()

        if (pingTimeMs == null) {
            LOG.warn("Got malformed pong: {}", event.payload)
            return
        }

        // Only update when the pong looks correct
        synchronized(this) {
            mostRecentPongReceivedAt = System.currentTimeMillis()
        }

        val currentTimeMs = System.currentTimeMillis()
        val timeSincePingMs = currentTimeMs - pingTimeMs
        LOG.debug("Received pong {} ms after ping (ping={}, pong={})", timeSincePingMs, pingTimeMs, mostRecentPongReceivedAt)
    }

    private fun onUnhealthy() {
        shouldRun = false

        LOG.info("WebSocket has been declared unhealthy, closing")
        webSocket.close(1000, null)
    }
}


class SlackRtmSession(val slackClient: SlackClient, val eventHandler: RtmEventHandler) {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    fun run() {
        val rtmStartResponse = slackClient.getRtmStartResponse()
        LOG.info("RTM Start response: {}", rtmStartResponse.self)

        slackClient.httpClient.newWebSocket(Request.Builder()
                .url(rtmStartResponse.url)
                .build(), JsonParsingListener(eventHandler))

        // Tell the executor to shutdown (otherwise, we're waiting for it to timeout)
        slackClient.httpClient.dispatcher().executorService().shutdown()
    }

}