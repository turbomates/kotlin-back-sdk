package dev.tmsoft.lib.saga

import dev.tmsoft.lib.exposed.type.jsonb
import java.time.LocalDateTime
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.datetime

data class Saga<T : Saga.Data>(val id: SagaId, val data: T) {
    val timeout: Int = 0
    val status: Status = Status.ACTIVE

    enum class Status {
        ACTIVE, TIMEOUT, ERROR, SUCCESS
    }

    interface Data {
        val key: Saga.Key
    }

    interface Key {
        val name: String
    }
}

data class SagaId(val id: String, val key: Saga.Key)

object SagaTable : IdTable<String>("sagas") {
    val name = varchar("name", 255)
    val timeout = integer("timeout").nullable()
    val metadata = jsonb("metadata", SagaSerializer)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    val status = enumerationByName("status", 25, Saga.Status::class).default(Saga.Status.ACTIVE)
    override val id: Column<EntityID<String>> = varchar("id", 255).entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}

object SagaSerializer : KSerializer<Saga.Data> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SagaSerializerDescriptor") {
        element("type", String::class.serializer().descriptor)
        element(
            "value",
            buildSerialDescriptor("kotlinx.serialization.Polymorphic<dev.tmsoft.lib.saga.Saga>", SerialKind.CONTEXTUAL)
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Saga.Data) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value::class.qualifiedName!!)
            encodeSerializableElement(descriptor, 1, value::class.serializer() as KSerializer<Saga.Data>, value)
        }
    }

    override fun deserialize(decoder: Decoder): Saga.Data {
        var value: Saga.Data? = null
        decoder.decodeStructure(descriptor) {
            var klassName: String? = null
            mainLoop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> {
                        break@mainLoop
                    }
                    0 -> {
                        klassName = decodeStringElement(descriptor, index)
                    }
                    1 -> {
                        klassName = requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
                        value = decodeSerializableElement(descriptor, index, getDeserializer(klassName))
                    }
                    else -> throw SerializationException(
                        "Invalid index in polymorphic deserialization of " +
                            (klassName ?: "unknown class") +
                            "\n Expected 0, 1 or DECODE_DONE(-1), but found $index"
                    )
                }
            }
        }
        return value!!
    }

    @Suppress("UNCHECKED_CAST")
    private fun getDeserializer(className: String): DeserializationStrategy<Saga.Data> {
        val klass = Class.forName(className).kotlin
        return klass.serializer() as DeserializationStrategy<Saga.Data>
    }
}
