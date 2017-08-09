package net.hanekawa.papika.common.slack


data class Self(
        val created: Int,
        val id: String,
        val name: String
)


data class RtmStartResponse(
        val ok: Boolean,
        val self: Self,
        val url: String
        )