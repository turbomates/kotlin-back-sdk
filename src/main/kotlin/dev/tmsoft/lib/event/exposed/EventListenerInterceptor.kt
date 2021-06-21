package dev.tmsoft.lib.event.exposed

import dev.tmsoft.lib.event.Event
import dev.tmsoft.lib.event.EventStore
import dev.tmsoft.lib.event.EventWrapper
import dev.tmsoft.lib.event.Events
import dev.tmsoft.lib.event.SubscriberWorker
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.statements.GlobalStatementInterceptor
import org.jetbrains.exposed.sql.statements.StatementInterceptor
import org.jetbrains.exposed.sql.transactions.transactionScope

class EventListenerInterceptor : GlobalStatementInterceptor {
    override fun beforeCommit(transaction: Transaction) {
        val events = transaction.events.raiseEvents().toList()
        events.save()
        if (events.isNotEmpty()) {
            transaction.registerInterceptor(PublishEventsInterceptor(events))
        }
    }
}

private class PublishEventsInterceptor(val events: List<Event>) : StatementInterceptor {
    override fun afterCommit() {
        runBlocking {
            events.forEach { event ->
                SubscriberWorker.eventsFlow.emit(Pair(event.eventId, event))
            }
        }
    }
}

val Transaction.events: EventStore by transactionScope { EventStore() }
internal fun List<Event>.save() {
    Events.batchInsert(this.toList()) { event ->
        this[Events.id] = event.eventId
        this[Events.event] = EventWrapper(event)
    }
}
