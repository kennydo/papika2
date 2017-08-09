package net.hanekawa.papika.common.slack

import com.squareup.moshi.Moshi
import net.hanekawa.papika.common.logging.getLogger
import net.hanekawa.papika.common.slack.errors.SlackConnectionError
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException


class SlackClient(val accessToken: String) {
    companion object {
        val LOG = getLogger(this::class.java)
        val baseUrl = HttpUrl.parse("https://slack.com/api/")
    }

    val httpClient = OkHttpClient.Builder().build()!!
    val moshi = Moshi.Builder().build()!!

    fun getRtmStartResponse(): RtmStartResponse {
        val url = baseUrl
                ?.newBuilder("rtm.start")
                ?.addQueryParameter("token", accessToken)
                ?.build()

        val request = Request.Builder()
                .url(url)
                .build()

        val call = httpClient.newCall(request)

        val response = try {
            call.execute()
        } catch (e: IOException) {
            throw SlackConnectionError("Unable to connect to Slack API")
        }

        if (response.code() != 200) {
            throw SlackConnectionError("Received non-200 response from Slack API")
        }

        val responseBody = response.body()?.source()

        val rtmStartResponse = moshi.adapter(RtmStartResponse::class.java).fromJson(responseBody)
                ?: throw SlackConnectionError("Could not parse: " + responseBody)

        return rtmStartResponse
    }

    fun getRtmSessionBuilder(): SlackRtmSessionBuilder {
        return SlackRtmSessionBuilder(this)
    }
}