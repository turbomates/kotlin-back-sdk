package dev.tmsoft.lib.event

import kotlin.reflect.full.companionObjectInstance

interface EventsSubscriber {
    fun subscribers(): List<EventSubscriberItem<out Event>>
    data class EventSubscriberItem<T : Event>(val key: Event.Key<T>, val subscriber: EventSubscriber<T>)

    infix fun <TEvent : Event, TKey : Event.Key<TEvent>, TSubscriber : EventSubscriber<TEvent>> TKey.to(that: TSubscriber): EventSubscriberItem<TEvent> =
        EventSubscriberItem(this, that)
}

interface EventSubscriber<T : Event> {
    suspend operator fun invoke(event: T)
}

interface EventSubscribers {

    fun subscribe(subscriber: EventsSubscriber)

    fun <T : Event> subscribe(key: Event.Key<T>, subscriber: EventSubscriber<T>)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Event> EventsSubscribers.subscribe(subscriber: EventSubscriber<T>) {
    val key = T::class.companionObjectInstance as? Event.Key<T>
    if (key == null && T::class != Event::class) {
        throw InvalidKeyException("wrong subscriber")
    }
    key?.let { subscribe(it, subscriber) }
}
