@file:UseSerializers(ResponseErrorSerializer::class)

package dev.tmsoft.lib.ktor

import dev.tmsoft.lib.query.paging.ContinuousList
import dev.tmsoft.lib.query.paging.ContinuousListSerializer
import dev.tmsoft.lib.serialization.resolveSerializer
import dev.tmsoft.lib.validation.Error
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.ApplicationSendPipeline
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.collections.set

@Serializable
sealed class Response {
    @Serializable
    class Error(val error: dev.tmsoft.lib.validation.Error) : Response()

    @Serializable(with = ResponseOkSerializer::class)
    object Ok : Response()

    @Serializable(with = ResponseEmptySerializer::class)
    object Empty : Response()

    class Redirect(val url: String) : Response()

    @Serializable(with = ResponseDataSerializer::class)
    class Data<T : Any>(val data: T) : Response()

    class File(val file: java.io.File) : Response()

    @Serializable
    class Errors(val errors: List<dev.tmsoft.lib.validation.Error>) : Response()

    @Serializable(with = ResponseListingSerializer::class)
    class Listing<T : Any>(val list: ContinuousList<T>) : Response()

    @Serializable(with = ResponseEitherSerializer::class)
    class Either<TL : Response, TR : Response>(val data: dev.tmsoft.lib.structure.Either<TL, TR>) : Response()
}

class RouteResponseInterceptor : Interceptor() {
    override fun intercept(route: Route) {
        route.sendPipeline.intercept(ApplicationSendPipeline.Before) { subject ->
            if (subject is Response.File) {
                context.response.status(subject.status(call.response.status()))
                call.respondFile(subject.file)
                finish()
            }
            if (subject is Response.Redirect) {
                call.respondRedirect(subject.url)
                finish()
            }
            if (subject is Response) {
                context.response.status(subject.status(call.response.status()))
                proceedWith(subject)
            }
        }
    }
}

fun Response.status(currentStatus: HttpStatusCode?): HttpStatusCode {
    return when (this) {
        is Response.Empty -> HttpStatusCode.NoContent
        is Response.Error -> if (currentStatus == HttpStatusCode.OK || currentStatus == null) HttpStatusCode.UnprocessableEntity else currentStatus
        is Response.Errors -> if (currentStatus == HttpStatusCode.OK || currentStatus == null) HttpStatusCode.UnprocessableEntity else currentStatus
        is Response.Either<*, *> -> this.data.fold(
            { it.status(currentStatus) },
            { it.status(currentStatus) }
        ) as HttpStatusCode
        is Response.Data<*> -> currentStatus ?: HttpStatusCode.OK
        is Response.Ok, is Response.Redirect, is Response.File, is Response.Listing<*> -> HttpStatusCode.OK
    }
}

object ResponseEitherSerializer : KSerializer<Response.Either<out Response, out Response>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ResponseEitherSerializerDescriptor")

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Response.Either<out Response, out Response>) {
        val output = encoder as? JsonEncoder ?: throw SerializationException(JSON_EXCEPTION_MESSAGE)
        val anon = { response: Response ->
            output.json.encodeToJsonElement(
                resolveSerializer(response) as KSerializer<Response>,
                response
            )
        }
        val tree: JsonElement = value.data.fold(anon, anon) as JsonElement

        output.encodeJsonElement(tree)
    }

    override fun deserialize(decoder: Decoder): Response.Either<*, *> {
        throw NotImplementedError()
    }
}

object ResponseErrorSerializer : KSerializer<Error> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ResponseErrorSerializerDescriptor")

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Error) {
        val output = encoder as? JsonEncoder ?: throw SerializationException(JSON_EXCEPTION_MESSAGE)
        val error: MutableMap<String, JsonElement> = mutableMapOf("message" to JsonPrimitive(value.message))
        if (value.property != null && value.property.isNotBlank()) error["property"] = JsonPrimitive(value.property)
        if (value.value != null) {
            error["value"] = output.json.encodeToJsonElement(
                resolveSerializer(value.value) as KSerializer<Any>,
                value.value
            )
        }

        val tree = JsonObject(error)
        output.encodeJsonElement(tree)
    }

    override fun deserialize(decoder: Decoder): Error {
        throw NotImplementedError()
    }
}

object ResponseOkSerializer : KSerializer<Response.Ok> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ResponseOkSerializerDescriptor")

    override fun serialize(encoder: Encoder, value: Response.Ok) {
        val output = encoder as? JsonEncoder ?: throw SerializationException(JSON_EXCEPTION_MESSAGE)
        output.encodeJsonElement(JsonObject(mapOf("data" to JsonPrimitive("ok"))))
    }

    override fun deserialize(decoder: Decoder): Response.Ok {
        throw NotImplementedError()
    }
}

object ResponseDataSerializer : KSerializer<Response.Data<Any>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ResponseDataSerializerDescriptor")

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Response.Data<Any>) {
        // toDo bug with inline classed and encodeToJsonElement
        val output = encoder as? JsonEncoder ?: throw SerializationException(JSON_EXCEPTION_MESSAGE)
        val encoded = output.json.encodeToJsonElement(
            resolveSerializer(value.data) as KSerializer<Any>,
            value.data
        )
        output.encodeJsonElement(
            JsonObject(
                mapOf(
                    "data" to encoded
                )
            )
        )
    }

    override fun deserialize(decoder: Decoder): Response.Data<Any> {
        throw NotImplementedError()
    }
}

object ResponseListingSerializer : KSerializer<Response.Listing<Any>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ResponseListingSerializerDescriptor")

    override fun serialize(encoder: Encoder, value: Response.Listing<Any>) {
        // toDo bug with inline classed and encodeToJsonElement
        val output = encoder as? JsonEncoder ?: throw SerializationException(JSON_EXCEPTION_MESSAGE)
        output.encodeJsonElement(output.json.encodeToJsonElement(ContinuousListSerializer, value.list))
    }

    override fun deserialize(decoder: Decoder): Response.Listing<Any> {
        throw NotImplementedError()
    }
}

object ResponseEmptySerializer : KSerializer<Response.Empty> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ResponseEmptySerializerDescriptor")

    override fun serialize(encoder: Encoder, value: Response.Empty) {
        val output = encoder as? JsonEncoder ?: throw SerializationException(JSON_EXCEPTION_MESSAGE)
        output.encodeJsonElement(JsonNull)
    }

    override fun deserialize(decoder: Decoder): Response.Empty {
        throw NotImplementedError()
    }
}

private const val JSON_EXCEPTION_MESSAGE = "This class can be saved only by Json"
