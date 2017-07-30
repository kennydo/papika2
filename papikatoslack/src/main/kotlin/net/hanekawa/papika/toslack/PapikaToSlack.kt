package net.hanekawa.papika.toslack

import net.hanekawa.papika.common.logging.getLogger


class PapikaToSlackBridge {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    fun run() {
        LOG.info("Hello world, I'm in the to-slack bridge")
    }
}


fun main(args: Array<String>) {
    val bridge = PapikaToSlackBridge()
    bridge.run()
}
