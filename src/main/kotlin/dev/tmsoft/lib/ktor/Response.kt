package dev.tmsoft.lib.ktor

import dev.tmsoft.lib.query.ContinuousList
import dev.tmsoft.lib.query.ContinuousListSerializer
import dev.tmsoft.lib.serialization.resolveSerializer
import dev.tmsoft.lib.validation.Error
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.ApplicationSendPipeline
import io.ktor.response.respondFile
import io.ktor.routing.Route
import kotlin.collections.set
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

@Serializable(with = ResponseSerializer::class)
sealed class Response {
    @Serializable(with = ResponseSerializer::class)
    class Error(val error: dev.tmsoft.lib.validation.Error) : Response()

    @Serializable
    object Ok : Response()

    @Serializable(with = ResponseSerializer::class)
    class Data<T : Any>(val data: T) : Response()

    @Serializable(with = ResponseSerializer::class)
    class File(val file: java.io.File) : Response()

    @Serializable(with = ResponseSerializer::class)
    class Errors(val errors: List<dev.tmsoft.lib.validation.Error>) : Response()

    @Serializable(with = ResponseSerializer::class)
    class Listing<T : Any>(val list: ContinuousList<T>) : Response()

    @Serializable(with = ResponseSerializer::class)
    class Either<TL : Response, TR : Response>(val data: dev.tmsoft.lib.structure.Either<TL, TR>) : Response()
}

class RouteResponseInterceptor : Interceptor() {
    override fun intercept(route: Route) {
        route.sendPipeline.intercept(ApplicationSendPipeline.Before) {
            if (it is Response.File) {
                context.response.status(it.status())
                call.respondFile(it.file)
                finish()
            }
        }
        route.sendPipeline.intercept(ApplicationSendPipeline.Transform) {
            if (it is Response) {
                context.response.status(it.status())
                proceedWith(it)
            }
        }
    }
}

fun Response.status(): HttpStatusCode {
    return when (this) {
        is Response.Error -> HttpStatusCode.UnprocessableEntity
        is Response.Errors -> HttpStatusCode.UnprocessableEntity
        is Response.Either<*, *> -> this.data.fold({ it.status() }, { it.status() }) as HttpStatusCode
        else -> HttpStatusCode.OK
    }
}

object ResponseSerializer : KSerializer<Response> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ResponseSerializerDescriptor")

    @InternalSerializationApi
    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Response) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
        val tree: JsonElement = when (value) {
            is Response.Error -> {
                JsonObject(mapOf("error" to output.json.encodeToJsonElement(value.error)))
            }
            is Response.Ok -> {
                JsonObject(mapOf("data" to JsonPrimitive("ok")))
            }
            is Response.Data<*> -> {
                // toDo bug with inline classed and encodeToJsonElement
                val encoded = output.json.encodeToString(
                    resolveSerializer(value.data) as KSerializer<Any>,
                    value.data
                )
                JsonObject(
                    mapOf(
                        "data" to output.json.parseToJsonElement(encoded)
                    )
                )
            }
            is Response.Listing<*> -> {
                output.json.encodeToJsonElement(ContinuousListSerializer, value.list)
            }
            is Response.Errors -> {
                JsonObject(
                    mapOf(
                        "errors" to output.json.encodeToJsonElement(
                            ListSerializer(Error.serializer()),
                            value.errors
                        )
                    )
                )
            }
            is Response.Either<*, *> -> {
                val anon = { response: Response -> output.json.encodeToJsonElement(ResponseSerializer, response) }
                value.data.fold(anon, anon) as JsonObject
            }
            else -> throw Exception("Response serialization: shouldn't reach here")
        }
        output.encodeJsonElement(tree)
    }

    override fun deserialize(decoder: Decoder): Response {
        throw NotImplementedError()
    }
}

object ErrorSerializer : KSerializer<Error> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ErrorSerializerDescriptor")

    @InternalSerializationApi
    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Error) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
        val error: MutableMap<String, JsonElement> = mutableMapOf("message" to JsonPrimitive(value.message))
        if (value.property != null && value.property.isNotBlank()) error["property"] = JsonPrimitive(value.property)
        if (value.value != null)
            error["value"] = output.json.encodeToJsonElement(
                resolveSerializer(value.value) as KSerializer<Any>,
                value.value
            )

        val tree = JsonObject(error)
        output.encodeJsonElement(tree)
    }

    override fun deserialize(decoder: Decoder): Error {
        throw NotImplementedError()
    }
}
