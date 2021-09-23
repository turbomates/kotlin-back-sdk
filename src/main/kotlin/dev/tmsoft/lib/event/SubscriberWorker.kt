package dev.tmsoft.lib.event

import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class SubscriberWorker(private val dao: EventsDatabaseAccess, private val publishers: List<Publisher>) :
    CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val eventDelay: Long = 10
    private val repeatDelay: Long = 10000
    suspend fun start() {
        launch {
            while (isActive) {
                val events = dao.load(eventDelay)
                events.forEach { (id, event) ->
                    publishers.forEach { publisher ->
                        logger.debug("event " + id.toString() + " was published by in worker ${publisher.javaClass.name}")
                        publisher.publish(event)
                    }
                    dao.publish(id)
                }
                delay(repeatDelay)
            }
        }
        launch {
            try {
                eventsFlow.collect { (id, event) ->
                    publishers.forEach { publisher ->
                        logger.debug("event " + id.toString() + " was published by in real-time ${publisher.javaClass.name}")
                        publisher.publish(event)
                    }
                    dao.publish(id)
                }
            } catch (logging: Throwable) {
                logger.error("Exception from the real time event publishing: $logging")
            }
        }
    }

    companion object {
        val eventsFlow: MutableSharedFlow<Pair<UUID, Event>> = MutableSharedFlow(extraBufferCapacity = 10)
    }
}
