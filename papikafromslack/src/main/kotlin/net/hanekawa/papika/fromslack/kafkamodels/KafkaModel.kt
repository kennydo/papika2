package net.hanekawa.papika.fromslack.kafkamodels

abstract class KafkaEventModel(val type: String, val subtype: String? = null)