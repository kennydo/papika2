package net.hanekawa.papika.fromslack

import net.hanekawa.papika.common.logging.getLogger
import net.hanekawa.papika.common.slack.SlackClient

class PapikaFromSlackBridge(val config: Config) {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    fun run() {
        LOG.info("Hello world, I'm in the from-slack bridge")
        LOG.info("Config: {}", config.toString())

        val slackClient = SlackClient(config.slackApiToken)

        slackClient.getRtmSessionBuilder().build().run()
    }
}


fun main(args: Array<String>) {
    val config = Config.load()
    val bridge = PapikaFromSlackBridge(config)
    bridge.run()
}
