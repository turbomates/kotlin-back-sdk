package dev.tmsoft.lib.event

class EventStore {
    private val events: MutableList<Pair<Event, Any?>> = mutableListOf()
    fun addEvent(event: Event) {
        events.add(event to null)
    }

    fun addEvent(event: Event, id: Any) {
        events.add(event to id)
    }

    fun raiseEvents(): Sequence<Pair<Event, Any?>> = sequence {
        events.forEach {
            yield(it)
        }
        events.clear()
    }
}
