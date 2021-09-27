package dev.tmsoft.lib.event

import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

@Serializable(with = EventWrapperSerializer::class)
internal data class EventWrapper(@Polymorphic val event: Event)

internal object EventWrapperSerializer : KSerializer<EventWrapper> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("EventDescriptor")

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): EventWrapper {
        val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be loaded only by Json")
        val tree = input.decodeJsonElement() as? JsonObject ?: throw SerializationException("Expected JsonObject")
        val type: KClass<Event> = Class.forName(tree.getValue("type").jsonPrimitive.content).kotlin as KClass<Event>
        val body: Event = input.json.decodeFromJsonElement(type.serializer(), tree.getValue("body").jsonObject)

        return EventWrapper(body)
    }

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: EventWrapper) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
        val tree = JsonObject(
            mapOf(
                "type" to JsonPrimitive(value.event::class.qualifiedName!!),
                "body" to output.json.encodeToJsonElement(
                    value.event::class.serializer() as KSerializer<Event>,
                    value.event
                )
            )
        )
        output.encodeJsonElement(tree)
    }
}
