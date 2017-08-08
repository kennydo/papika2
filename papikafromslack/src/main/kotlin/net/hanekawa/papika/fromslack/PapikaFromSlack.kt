package net.hanekawa.papika.fromslack

import com.google.gson.GsonBuilder
import com.ullink.slack.simpleslackapi.events.*
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import net.hanekawa.papika.common.logging.getLogger
import net.hanekawa.papika.fromslack.kafkamodels.events.*

class PapikaFromSlackBridge(val config: Config) {
    companion object {
        val LOG = getLogger(this::class.java)
    }

    private val slackSession = SlackSessionFactory.createWebSocketSlackSession(config.slackApiToken)
    private val gsonBuilder = GsonBuilder()
    private val gson = gsonBuilder.create()

    fun run() {
        LOG.info("Hello world, I'm in the from-slack bridge")
        LOG.info("Config: {}", config.toString())

        registerSlackListeners()

        slackSession.connect()
    }

    private fun registerSlackListeners() {

        slackSession.addPinAddedListener { event: PinAdded, _ ->
        }
        slackSession.addPinRemovedListener { event: PinRemoved, _ -> }
        slackSession.addPresenceChangeListener { event: PresenceChange, _ -> }
        slackSession.addReactionAddedListener { event: ReactionAdded, _ -> }
        slackSession.addReactionRemovedListener { event: ReactionRemoved, _ -> }
        slackSession.addChannelArchivedListener { event: SlackChannelArchived, _ -> }
        slackSession.addChannelCreatedListener { event: SlackChannelCreated, _ -> }
        slackSession.addChannelDeletedListener { event: SlackChannelDeleted, _ -> }
        slackSession.addChannelJoinedListener { event: SlackChannelJoined, _ -> }
        slackSession.addChannelLeftListener { event: SlackChannelLeft, _ -> }
        slackSession.addChannelRenamedListener { event: SlackChannelRenamed, _ -> }
        slackSession.addChannelUnarchivedListener { event: SlackChannelUnarchived, _ -> }
        slackSession.addSlackConnectedListener { event: SlackConnected, _ -> }
        slackSession.addSlackDisconnectedListener { event: SlackDisconnected, _ -> }
        slackSession.addGroupJoinedListener { event: SlackGroupJoined, _ -> }
        slackSession.addMessageDeletedListener { event: SlackMessageDeleted, _ -> }
        slackSession.addMessagePostedListener { event: SlackMessagePosted, _ -> }
        slackSession.addMessageUpdatedListener { event: SlackMessageUpdated, _ ->
            val json = gson.toJson(MessageUpdatedModel.extract(event))
            println(json)
        }
        slackSession.addUserTypingListener { event: UserTyping, _ ->
            val json = gson.toJson(UserTypingModel.extract(event))
            println(json)
        }
    }
}


fun main(args: Array<String>) {
    val config = Config.load()
    val bridge = PapikaFromSlackBridge(config)
    bridge.run()
}
