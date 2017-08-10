package net.hanekawa.papika.common.slack

enum class EventType(val eventName: String) {
    USER_TYPING("user_typing"),
    MESSAGE("message"),
    MEMBER_JOINED_CHANNEL("member_joined_channel"),
    MEMBER_LEFT_CHANNEL("member_left_channel"),

    UNKNOWN("unknown");

    companion object {
        private val map = EventType.values().associateBy(EventType::eventName)
        fun fromEventName(eventName: String?): EventType? = map[eventName]
    }
}