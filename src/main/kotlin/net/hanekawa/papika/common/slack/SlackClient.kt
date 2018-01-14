package net.hanekawa.papika.common.slack

import com.squareup.moshi.Moshi
import com.timgroup.statsd.StatsDClient
import net.hanekawa.papika.common.getLogger
import net.hanekawa.papika.common.slack.errors.SlackConnectionError
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException


class SlackClient(private val statsd: StatsDClient, private val accessToken: String) {
    companion object {
        val LOG = getLogger(this::class.java)
        val baseUrl = HttpUrl.parse("https://slack.com/api/")
    }

    val httpClient = OkHttpClient.Builder().build()!!
    val moshi = Moshi.Builder().build()!!
    private var mapAdapter = moshi.adapter(Map::class.java)

    fun getRtmStartResponse(): RtmStartResponse {
        val url = baseUrl
                ?.newBuilder("rtm.start")
                ?.addQueryParameter("token", accessToken)
                ?.build()!!

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

        val responseBody = response.body()!!.source()

        return moshi.adapter(RtmStartResponse::class.java).fromJson(responseBody)
                ?: throw SlackConnectionError("Could not parse: " + responseBody)
    }

    fun buildRtmSession(eventHandler: RtmEventHandler): SlackRtmSession {
        return SlackRtmSession(statsd, this, eventHandler)
    }

    fun callApi(apiMethod: String, payload: Map<String, String?>): Map<String, Any>? {
        val apiUrl = baseUrl
                ?.newBuilder(apiMethod)
                ?.addQueryParameter("token", accessToken)
                ?.build()!!

        val formBodyBuilder = FormBody.Builder()
        payload.forEach { entry ->
            if (entry.value != null) {
                formBodyBuilder.add(entry.key, entry.value!!)
            }
        }

        val request = Request
                .Builder()
                .url(apiUrl)
                .post(formBodyBuilder.build())
                .build()
        val call = httpClient.newCall(request)

        val response = try {
            call.execute()
        } catch (e: IOException) {
            LOG.error("Could not send request to Slack: {}", e)
            null
        } ?: return null

        return try {
            @Suppress("UNCHECKED_CAST")
            mapAdapter.fromJson(response.body()?.source()!!) as? Map<String, Any>
        } catch (e: IOException) {
            LOG.error("Unable to parse response from Slack: {}", e)
            null
        }
    }
}