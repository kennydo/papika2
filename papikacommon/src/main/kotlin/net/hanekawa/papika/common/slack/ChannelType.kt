package net.hanekawa.papika.common.slack

enum class ChannelType(val channelType: String) {
    PUBLIC("C"),
    PRIVATE("G");

    companion object {
        private val map = ChannelType.values().associateBy(ChannelType::channelType)
        fun fromChannelType(channelType: String) = map[channelType]
    }
}