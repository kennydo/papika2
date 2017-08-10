package net.hanekawa.papika.common.slack.eventmodels

import com.squareup.moshi.Json
import net.hanekawa.papika.common.slack.ChannelType
import net.hanekawa.papika.common.slack.EventType

data class MemberLeftChannelEvent(
        val user: String,
        val channel: String,
        @Json(name = "channel_type") val channelType: ChannelType,
        val inviter: String
) : EventModel(EventType.USER_TYPING)