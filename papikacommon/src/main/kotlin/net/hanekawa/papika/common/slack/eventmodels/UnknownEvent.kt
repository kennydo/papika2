package net.hanekawa.papika.common.slack.eventmodels

import net.hanekawa.papika.common.slack.EventType

data class UnknownEvent(
        val jsonObject: Map<String, Any>
) : EventModel(EventType.UNKNOWN)