package dev.tmsoft.lib.event

import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import dev.tmsoft.lib.extension.camelToSnakeCase
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class RabbitEvents(
    private val connection: Connection,
    private val exchange: String,
    private val prefix: String,
    private val json: Json
) :
    Publisher,
    EventSubscribers {
    private val channel: Channel = connection.createChannel()

    init {
        channel.declareLocalExchange(exchange)
    }

    override suspend fun publish(event: Event): Boolean {
        channel.basicPublish(
            exchange,
            event.key.routeName(),
            null,
            json.encodeToString(EventWrapper.serializer(), EventWrapper(event)).toByteArray()
        )
        return true
    }

    override fun subscribe(subscriber: EventsSubscriber) {
        val channel = connection.createChannel()
        val queueName = subscriber.queueName(prefix)
        channel.queueDeclare(queueName, true, false, false, mapOf())
        subscriber.subscribers().forEach { (key, _) ->
            channel.queueBind(queueName, exchange, key.routeName())
        }
        channel.basicConsume(
            queueName,
            false,
            ListenerDeliveryCallback(
                channel,
                subscriber.subscribers().associate { it.key to it.subscriber },
                json
            ),
            ListenerCancelCallback()
        )
    }

    override fun <T : Event> subscribe(key: Event.Key<T>, subscriber: EventSubscriber<T>) {
        val channel = connection.createChannel()
        val queueName = subscriber.queueName(prefix)
        channel.queueDeclare(queueName, true, false, false, mapOf())
        channel.queueBind(queueName, exchange, key.routeName())
        channel.basicConsume(
            queueName,
            false,
            ListenerDeliveryCallback(channel, mapOf(key to subscriber), json),
            ListenerCancelCallback()
        )
    }

    private class ListenerDeliveryCallback(
        private val channel: Channel,
        private val subscribers: Map<Event.Key<out Event>, EventSubscriber<out Event>>,
        private val json: Json
    ) : DeliverCallback {
        private val logger by lazy { LoggerFactory.getLogger(javaClass) }

        @Suppress("UNCHECKED_CAST")
        override fun handle(consumerTag: String, message: Delivery) = runBlocking {
            try {
                val eventJsonString = String(message.body)
                logger.info("Event $eventJsonString accepted ")
                val event = json.decodeFromString(EventWrapper.serializer(), eventJsonString).event

                val callback = subscribers[event.key] as? EventSubscriber<Event>
                callback?.invoke(event)
                channel.basicAck(message.envelope.deliveryTag, false)
            } catch (logging: Throwable) {
                channel.basicNack(message.envelope.deliveryTag, false, true)
                logger.error("Broken event: ${String(message.body)}. Message: ${logging.message}.")
            }
        }
    }

    private class ListenerCancelCallback : CancelCallback {
        private val logger by lazy { LoggerFactory.getLogger(javaClass) }
        override fun handle(consumerTag: String?) {
            logger.error("Listener was cancelled $consumerTag")
        }
    }
}

private fun Event.Key<*>.routeName(): String {
    val packagePath = this::class.qualifiedName!!.split('.').dropLast(1)
    return packagePath.takeLast(3).joinToString(".").camelToSnakeCase()
}

private fun Channel.declareLocalExchange(exchange: String) {
    exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, true)
}

private fun EventSubscriber<*>.queueName(prefix: String): String {
    val packagePath = this::class.qualifiedName!!
    val splitPath = packagePath.split('.')
    return prefix + "." + splitPath.getOrNull(2)?.lowercase() + "." + splitPath.takeLast(1)
        .joinToString(separator = ".") { it.camelToSnakeCase() }
}

private fun EventsSubscriber.queueName(prefix: String): String {
    val packagePath = this::class.qualifiedName!!
    val splitPath = packagePath.split('.')
    return prefix + "." + splitPath.getOrNull(2)?.lowercase() + "." + splitPath.takeLast(1)
        .joinToString(separator = ".") { it.camelToSnakeCase() }
}
