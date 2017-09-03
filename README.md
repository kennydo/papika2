# papika2
Slack-Kafka bridge, written in Kotlin

# Background

Slack lets you make cool integrations, but creating new integrations is a tedious process. By broadcasting Slack events over Kafka and conversely sending message to Slack via Kafka, creating new integrations is a snap!

Papika lives as two components: a bridge that pipes events from Slack's real-time websocket API to Kafka, and another bridge that accepts Kafka records that look like calls to Slack's [`chat.PostMessage`](https://api.slack.com/methods/chat.postMessage) API and sends it to Slack.

For high availability, you can create as many to-slack bridges as you want (the only limit is the # of Kafka partitions you want to support for that topic). You can also run as many from-slack bridges as you want (but through leader election via zookeeper, only one will actively emit to the from-slack Kafka topic).
