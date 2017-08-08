package net.hanekawa.papika.fromslack.kafkamodels.events

import com.ullink.slack.simpleslackapi.events.SlackMessageUpdated
import net.hanekawa.papika.fromslack.kafkamodels.KafkaEventModel
import net.hanekawa.papika.fromslack.kafkamodels.SlackAttachmentModel
import net.hanekawa.papika.fromslack.kafkamodels.SlackChannelModel
import net.hanekawa.papika.fromslack.kafkamodels.SlackEventExtractor

class MessageUpdatedModel(
        val channel: SlackChannelModel,
        val timestamp: String,
        val message: String,
        val messageTimestamp: String,
        val attachments: Array<SlackAttachmentModel>
) : KafkaEventModel(type = "message", subtype = "message_changed") {
    companion object : SlackEventExtractor<SlackMessageUpdated, MessageUpdatedModel> {
        override fun extract(event: SlackMessageUpdated): MessageUpdatedModel {
            return MessageUpdatedModel(
                    channel=SlackChannelModel.extract(event.channel),
                    timestamp=event.timeStamp,
                    message=event.newMessage,
                    messageTimestamp=event.messageTimestamp,
                    attachments=event.attachments.map { SlackAttachmentModel.extract(it) }.toTypedArray()
            )
        }
    }
}