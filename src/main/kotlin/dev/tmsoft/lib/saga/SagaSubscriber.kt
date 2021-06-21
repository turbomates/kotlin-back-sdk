package dev.tmsoft.lib.saga

import dev.tmsoft.lib.event.Event

interface SagaSubscriber<TSaga : Saga.Data> {
    fun rollback(saga: Saga<TSaga>)
    fun subscribers(): List<EventSubscriberItem<out Event, TSaga>>
    fun sagaId(event: Event): SagaId
    data class EventSubscriberItem<TEvent : Event, TSaga : Saga.Data>(
        val key: Event.Key<TEvent>,
        val subscriber: SagaEventSubscriber<TEvent, TSaga>
    )

    infix fun <TEvent : Event, TKey : Event.Key<TEvent>, TSubscriber : SagaEventSubscriber<TEvent, TSaga>> TKey.to(
        that: TSubscriber
    ): EventSubscriberItem<TEvent, TSaga> =
        EventSubscriberItem(this, that)
}

interface SagaEventSubscriber<TEvent : Event, TSaga : Saga.Data> {
    suspend operator fun invoke(saga: Saga<TSaga>, event: TEvent)
}
