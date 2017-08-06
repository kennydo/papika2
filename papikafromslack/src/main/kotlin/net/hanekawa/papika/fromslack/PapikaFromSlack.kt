package net.hanekawa.papika.fromslack

import com.ullink.slack.simpleslackapi.SlackSession
import com.ullink.slack.simpleslackapi.events.UserTyping
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import com.ullink.slack.simpleslackapi.listeners.UserTypingListener
import net.hanekawa.papika.common.logging.getLogger
import java.util.concurrent.TimeUnit

import javax.websocket.Endpoint


class Foo : UserTypingListener {
    override fun onEvent(event: UserTyping?, session: SlackSession?) {
        println(event.toString())
    }

}

class PapikaFromSlackBridge(val config: Config) {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    fun run() {
        LOG.info("Hello world, I'm in the from-slack bridge")
        LOG.info("Config: {}", config.toString())

        val slackSession = SlackSessionFactory.createWebSocketSlackSession(config.slackApiToken)
        slackSession.addUserTypingListener(Foo())
        slackSession.connect()
    }
}


fun main(args: Array<String>) {
    val config = Config.load()
    val bridge = PapikaFromSlackBridge(config)
    bridge.run()
}
