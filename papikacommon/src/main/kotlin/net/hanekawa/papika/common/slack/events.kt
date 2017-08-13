package net.hanekawa.papika.common.slack

import com.squareup.moshi.Json
import net.hanekawa.papika.common.slack.EventType


sealed class Event(val type: EventType)

sealed class MessageEvent(
        val subtype: MessageSubtype,
        val channel: String? = null,
        val user: String? = null,
        val text: String,
        val ts: String,
        val attachments: List<Attachment>? = null,
        val edited: EditedProperty? = null
) : Event(type = EventType.MESSAGE) {
    class EditedProperty(val user: String, val ts: String)
}


class Attachment(
        val fallback: String,
        val color: String,
        val pretext: String,
        @Json(name = "author_name") val authorName: String,
        @Json(name = "author_link") val authorLink: String,
        @Json(name = "author_icon") val authorIcon: String,
        val title: String,
        @Json(name = "title_link") val titleLink: String,
        val text: String,
        val fields: List<AttachmentField>,
        @Json(name = "image_url") val imageUrl: String,
        @Json(name = "thumb_url") val thumbUrl: String,
        val footer: String,
        @Json(name = "footer_icon") val footerIcon: String,
        val ts: Number
) {
    class AttachmentField(val title: String, val value: String, val short: Boolean)
}
