package dev.tmsoft.lib.event

import com.turbomates.time.OffsetDateTimeSerializer
import com.turbomates.time.nowUTC
import dev.tmsoft.lib.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.util.UUID

@Serializable
abstract class Event {
    @Serializable(with = UUIDSerializer::class)
    val eventId: UUID = UUID.randomUUID()

    @Serializable(with = OffsetDateTimeSerializer::class)
    val eventCreatedAt: OffsetDateTime = nowUTC
    abstract val key: Key<out Event>

    interface Key<T : Event>
}
