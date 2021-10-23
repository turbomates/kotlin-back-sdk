package dev.tmsoft.lib.exposed.dao

import dev.tmsoft.lib.event.Event
import dev.tmsoft.lib.event.EventStore
import dev.tmsoft.lib.event.exposed.events
import org.jetbrains.exposed.sql.transactions.TransactionManager

interface AggregateRoot {
    private val eventStore: EventStore
        get() = TransactionManager.current().events

    fun addEvent(event: Event) {
        eventStore.addEvent(event)
    }

    fun addEvent(event: Event, id: Any) {
        eventStore.addEvent(event, id)
    }
}

interface EventSourcedAggregateRoot<T : Comparable<T>> : AggregateRoot {
    var id: T
    override fun addEvent(event: Event) {
        super.addEvent(event, id)
    }
}
