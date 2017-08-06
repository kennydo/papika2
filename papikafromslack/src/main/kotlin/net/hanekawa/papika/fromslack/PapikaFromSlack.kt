package net.hanekawa.papika.fromslack

import net.hanekawa.papika.common.logging.getLogger

class PapikaFromSlackBridge {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    fun run() {
        val config = Config.load()

        LOG.info("Hello world, I'm in the from-slack bridge")
        LOG.info("Config: {}", config.toString())
    }
}


fun main(args: Array<String>) {
    val bridge = PapikaFromSlackBridge()
    bridge.run()
}
