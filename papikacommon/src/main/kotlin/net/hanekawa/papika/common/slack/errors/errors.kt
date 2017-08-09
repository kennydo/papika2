package net.hanekawa.papika.common.slack.errors

class SlackLoginError : Exception()

class SlackConnectionError : Exception {
    constructor (message: String) : super(message)
}

