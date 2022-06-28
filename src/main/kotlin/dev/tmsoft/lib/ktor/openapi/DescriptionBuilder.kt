package dev.tmsoft.lib.ktor.openapi

import com.sksamuel.hoplite.simpleName
import com.turbomates.openapi.OpenApiKType
import com.turbomates.openapi.Property
import com.turbomates.openapi.Type
import com.turbomates.openapi.openApiKType
import dev.tmsoft.lib.ktor.Response
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.reflect.full.memberProperties

val responseMap: OpenApiKType.() -> Map<Int, Type> = {
    when (this) {
        Response.Ok::class.openApiKType(),
        Response.Listing::class.openApiKType(),
        Response.Data::class.openApiKType() -> mapOf(
            HttpStatusCode.OK.value to typeBuilder(this)
        )
        Response.Either::class.openApiKType() -> {
            val data = Response.Either::class.memberProperties.first()
            val result = mutableMapOf<Int, Type>()
            data.returnType.arguments.forEach { argument ->
                when (val projectionType = this.getArgumentProjectionType(argument.type!!)) {
                    Response.Ok::class.openApiKType() -> result[HttpStatusCode.OK.value] = typeBuilder(this)
                    Response.Error::class.openApiKType(), Response.Errors::class.openApiKType() -> mapOf(
                        result[HttpStatusCode.UnprocessableEntity.value] to typeBuilder(this)
                    )
                    else -> result[HttpStatusCode.OK.value] = projectionType.objectType(argument.type!!.simpleName)
                }
            }
            result
        }
        Response.Error::class.openApiKType(), Response.Errors::class.openApiKType() -> mapOf(
            HttpStatusCode.UnprocessableEntity.value to typeBuilder(this)
        )
        else -> mapOf(
            HttpStatusCode.OK.value to typeBuilder(this)
        )
    }
}
val typeBuilder: (OpenApiKType) -> Type.Object = { type ->
    when (type) {
        Response.Ok::class.openApiKType() -> Type.Object(
            "ok",
            listOf(
                Property(
                    "data",
                    Type.String()
                )
            ),
            example = buildJsonObject { put("data", "ok") },
            nullable = false
        )

        Response.Error::class.openApiKType() -> Type.Object(
            "error",
            listOf(
                Property(
                    "error",
                    Type.String()
                )
            ),
            example = buildJsonObject { put("error", "Wrong response") },
            nullable = false
        )

        else -> type.objectType()
    }
}
