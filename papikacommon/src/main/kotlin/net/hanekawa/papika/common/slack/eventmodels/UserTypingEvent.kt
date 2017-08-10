package net.hanekawa.papika.common.slack.eventmodels

import net.hanekawa.papika.common.slack.EventType

data class UserTypingEvent(
        val channel: String,
        val user: String
) : EventModel(EventType.USER_TYPING)