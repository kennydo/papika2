package net.hanekawa.papika.common.slack


class RtmEvent(val type: String, val payload: Map<String, Any>)