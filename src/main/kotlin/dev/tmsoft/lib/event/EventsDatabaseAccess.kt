package dev.tmsoft.lib.event

import dev.tmsoft.lib.exposed.TransactionManager
import dev.tmsoft.lib.exposed.type.jsonb
import java.time.LocalDateTime
import java.util.UUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.second
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

class EventsDatabaseAccess(private val transaction: TransactionManager) {
    suspend fun load(delay: Long): List<Pair<UUID, Event>> {
        return transaction {
            Events.select {
                (Events.publishedAt.isNull()) and (Events.createdAt.minus(LocalDateTime.now()).second() less -delay)
            }.map {
                it[Events.id].value to it[Events.event].event
            }
        }
    }

    suspend fun publish(id: UUID) {
        transaction {
            Events.update({ Events.id eq id }) {
                it[publishedAt] = LocalDateTime.now()
            }
        }
    }
}

internal object Events : UUIDTable("domain_events") {
    val event = jsonb("event", EventWrapper.serializer())
    val publishedAt = datetime("published_at").nullable()
    val createdAt = datetime("created_at")
}
