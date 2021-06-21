package dev.tmsoft.lib.exposed.dao

import dev.tmsoft.lib.event.Event
import dev.tmsoft.lib.event.exposed.events
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.TransactionManager

open class AggregateRoot<ID : Comparable<ID>>(id: EntityID<ID>) : Entity<ID>(id) {
    private var store = TransactionManager.current().events
    fun addEvent(event: Event) {
        store.addEvent(event)
    }
}
