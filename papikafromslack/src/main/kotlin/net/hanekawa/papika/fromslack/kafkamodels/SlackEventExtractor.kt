package net.hanekawa.papika.fromslack.kafkamodels

import com.ullink.slack.simpleslackapi.events.SlackEvent

interface SlackEventExtractor<in T: SlackEvent, out U: KafkaEventModel> {
    fun extract(event: T): U
}