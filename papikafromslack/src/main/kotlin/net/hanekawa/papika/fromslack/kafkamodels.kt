package net.hanekawa.papika.fromslack.kafkamodels



class PinAddedModel() : KafkaEventModel(type = "pin_added")
class PinRemovedModel() : KafkaEventModel(type = "pin_removed")
class PresenceChangeModel() : KafkaEventModel(type = "presence_change")
class ReactionAddedModel() : KafkaEventModel(type = "reaction_added")
class ReactionRemovedModel() : KafkaEventModel(type = "reaction_removed")
class ChannelArchivedModel() : KafkaEventModel(type = "channel_archive")
class ChannelCreatedModel() : KafkaEventModel(type = "channel_created")
class ChannelDeletedModel() : KafkaEventModel(type = "channel_deleted")
class ChannelJoinedModel() : KafkaEventModel(type = "channel_joined")
class ChannelLeftModel() : KafkaEventModel(type = "channel_left")
class ChannelRenamedModel() : KafkaEventModel(type = "channel_rename")
class ChannelUnarchivedModel() : KafkaEventModel(type = "channel_unarchive")
class HelloModel() : KafkaEventModel(type = "hello")
class GoodbyedModel() : KafkaEventModel(type = "goodbye")
class GroupJoinedModel() : KafkaEventModel(type = "group_joined")
class MessageDeletedModel() : KafkaEventModel(type = "message", subtype = "message_deleted")
class MessagePostedModel() : KafkaEventModel(type = "message")
