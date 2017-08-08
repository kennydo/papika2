package net.hanekawa.papika.fromslack.kafkamodels

import com.ullink.slack.simpleslackapi.SlackChannel

data class SlackChannelModel(
        val id: String,
        val name: String
) {
    companion object {
        fun extract(channel: SlackChannel): SlackChannelModel {
            return SlackChannelModel(
                    id = channel.id,
                    name = channel.name
            )
        }
    }
}