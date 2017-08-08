package net.hanekawa.papika.fromslack.kafkamodels.events

import com.ullink.slack.simpleslackapi.events.UserTyping
import net.hanekawa.papika.fromslack.kafkamodels.KafkaEventModel
import net.hanekawa.papika.fromslack.kafkamodels.SlackChannelModel
import net.hanekawa.papika.fromslack.kafkamodels.SlackEventExtractor
import net.hanekawa.papika.fromslack.kafkamodels.SlackUserModel

data class UserTypingModel(
        val user: SlackUserModel,
        val channel: SlackChannelModel
) : KafkaEventModel(type = "user_typing") {
    companion object : SlackEventExtractor<UserTyping, UserTypingModel> {
        override fun extract(event: UserTyping): UserTypingModel {
            return UserTypingModel(
                    user = SlackUserModel.extract(event.user),
                    channel = SlackChannelModel.extract(event.channel)
            )
        }
    }
}