package dev.tmsoft.lib.event

class EventStore {
    private val events: MutableList<Pair<Event, Any?>> = mutableListOf()
    fun addEvent(event: Event) {
        events.add(Pair(event, null))
    }

    fun raiseEvents(): Sequence<Pair<Event, Any?>> = sequence {
        events.forEach {
            yield(it)
        }
        events.clear()
    }
}
