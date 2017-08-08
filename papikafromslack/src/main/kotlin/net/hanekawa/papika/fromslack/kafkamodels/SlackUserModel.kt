package net.hanekawa.papika.fromslack.kafkamodels

import com.ullink.slack.simpleslackapi.SlackUser

enum class SlackPresence {
    UNKNOWN, ACTIVE, AWAY, AUTO
}

data class SlackUserModel(
        val id: String,
        val userName: String?,
        val realName: String?,
        val userMail: String?,
        val userTitle: String?,
        val isAdmin: Boolean,
        val isOwner: Boolean,
        val isPrimaryOwner: Boolean,
        val isBot: Boolean,
        val timeZone: String?,
        val timeZoneOffset: Int?,
        val presence: SlackPresence
) {
    companion object {
        fun extract(user: SlackUser): SlackUserModel {
            return SlackUserModel(
                    id = user.id,
                    userName = user.userName,
                    realName = user.realName,
                    userMail = user.userMail,
                    userTitle = user.userTitle,
                    isAdmin = user.isAdmin,
                    isOwner = user.isOwner,
                    isPrimaryOwner = user.isPrimaryOwner,
                    isBot = user.isBot,
                    timeZone = user.timeZone,
                    timeZoneOffset = user.timeZoneOffset,
                    presence = SlackPresence.valueOf(user.presence.name)
            )
        }
    }
}