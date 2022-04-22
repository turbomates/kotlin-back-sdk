package dev.tmsoft.lib.event

import dev.tmsoft.lib.exposed.TransactionManager
import dev.tmsoft.lib.exposed.type.jsonb
import java.time.LocalDateTime
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select

class EventSourcingAccess(private val transaction: TransactionManager) {
    suspend fun get(aggregateRoot: Any): List<Event> {
        return transaction {
            EventSourcingTable
                .select { EventSourcingTable.aggregateRoot eq aggregateRoot.toString() }
                .orderBy(EventSourcingTable.createdAt, SortOrder.ASC)
                .map { it[EventSourcingTable.event].event }
        }
    }
}

object EventSourcingTable : UUIDTable("event_sourcing") {
    val aggregateRoot = varchar("aggregate_root_id", 255)
    internal val event = jsonb("data", EventWrapper.serializer())
    val createdAt = datetime("created_at")
}
