package dev.tmsoft.lib.event

import io.sentry.Sentry
import kotlin.reflect.full.companionObjectInstance
import org.slf4j.LoggerFactory

class EventsSubscribers : EventSubscribers {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val subscribers: MutableMap<Event.Key<out Event>, MutableList<EventSubscriber<out Event>>> =
        mutableMapOf()

    override fun subscribe(subscriber: EventsSubscriber) {
        subscriber.subscribers().forEach {
            addToMap(it.key, it.subscriber)
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Event> subscribe(subscriber: EventSubscriber<T>) {
        val key = T::class.companionObjectInstance as? Event.Key<T>
        if (key == null && T::class != Event::class) {
            throw InvalidKeyException("wrong subscriber")
        }
        key?.let { subscribe(it, subscriber) }
    }

    override fun <T : Event> subscribe(key: Event.Key<T>, subscriber: EventSubscriber<T>) {
        addToMap(key, subscriber)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Event> call(event: T) {
        val subscribers = subscribers.getOrDefault(event.key, mutableListOf()) as MutableList<EventSubscriber<T>>
        subscribers.forEach {
            try {
                it(event)
            } catch (logging: Throwable) {
                Sentry.captureException(logging)
                logger.error(logging.message)
            }
        }
    }

    private fun addToMap(key: Event.Key<out Event>, subscriber: EventSubscriber<out Event>) {
        val list = subscribers.getOrPut(key) {
            mutableListOf()
        }
        list.add(subscriber)
    }
}
