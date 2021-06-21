package dev.tmsoft.lib.event

import dev.tmsoft.lib.date.LocalDateTimeSerializer
import dev.tmsoft.lib.serialization.UUIDSerializer
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
abstract class Event {
    @Serializable(with = UUIDSerializer::class)
    val eventId: UUID = UUID.randomUUID()

    @Serializable(with = LocalDateTimeSerializer::class)
    val eventCreatedAt: LocalDateTime = LocalDateTime.now()
    abstract val key: Key<out Event>

    interface Key<T : Event>
}
