package net.hanekawa.papika.fromslack.kafkamodels

import com.ullink.slack.simpleslackapi.SlackAttachment

data class SlackAttachmentModel(
        val title: String?,
        val titleLink: String?,
        val fallback: String?,
        val text: String?,
        val pretext: String?,
        val thumbUrl: String?,
        val imageUrl: String?,
        val authorName: String?,
        val authorLink: String?,
        val authorIcon: String?,
        val footer: String?,
        val footerIcon: String?
) {
    companion object {
        fun extract(attachment: SlackAttachment): SlackAttachmentModel {
            return SlackAttachmentModel(
                    title = attachment.title,
                    titleLink = attachment.titleLink,
                    fallback = attachment.fallback,
                    text = attachment.text,
                    pretext = attachment.pretext,
                    thumbUrl = attachment.thumbUrl,
                    imageUrl = attachment.imageUrl,
                    authorName = attachment.authorName,
                    authorLink = attachment.authorLink,
                    authorIcon = attachment.authorIcon,
                    footer = attachment.footer,
                    footerIcon = attachment.footerIcon
            )
        }
    }
}