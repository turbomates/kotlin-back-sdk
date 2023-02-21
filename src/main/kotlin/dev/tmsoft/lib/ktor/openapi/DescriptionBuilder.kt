package dev.tmsoft.lib.ktor.openapi

import com.turbomates.openapi.Property
import com.turbomates.openapi.Type
import dev.tmsoft.lib.ktor.Response
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

val responseMap: KType.() -> Map<Int, KType> = {
    if (this.isSubtypeOf(typeOf<Response.Either<Response, Response>>())) {
        val result = mutableMapOf<Int, KType>()
        jvmErasure.typeParameters.forEachIndexed { index, kTypeParameter ->
            result += arguments[index].type!!.simpleResponseMap()
        }
        result
    } else this.simpleResponseMap()
}

private val simpleResponseMap: KType.() -> Map<Int, KType> = {
    when {
        this == typeOf<Response.Ok>() -> mapOf(
            HttpStatusCode.OK.value to this
        )

        this.isSubtypeOf(typeOf<Response.Listing<*>>()) -> mapOf(
            HttpStatusCode.OK.value to this
        )

        this.isSubtypeOf(typeOf<Response.Data<*>>()) -> mapOf(
            HttpStatusCode.OK.value to this
        )

        this == typeOf<Response.Error>() || this == typeOf<Response.Errors>() -> mapOf(
            HttpStatusCode.UnprocessableEntity.value to this
        )

        else -> mapOf(
            HttpStatusCode.OK.value to this
        )
    }
}
val responseDescriptions = mapOf(
    typeOf<Response.Ok>() to Type.Object(
        "error",
        listOf(Property("error", Type.String())),
        example = buildJsonObject { put("error", "Wrong response") },
        nullable = false
    ),
    typeOf<Response.Error>() to Type.Object(
        "error",
        listOf(Property("error", Type.String())),
        example = buildJsonObject { put("error", "Wrong response") },
        nullable = false
    )
)
