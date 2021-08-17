package dev.tmsoft.lib.ktor.openapi

import dev.tmsoft.lib.ktor.Response
import dev.tmsoft.lib.openapi.OpenApiKType
import dev.tmsoft.lib.openapi.Property
import dev.tmsoft.lib.openapi.Type
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DescriptionBuilder(private val type: OpenApiKType) {
    fun buildResponseMap(): Map<Int, Type> {
        return when {
            type.jvmErasure.isSubclassOf(Response.Ok::class) -> mapOf(
                200 to getOkType()
            )
            type.jvmErasure.isSubclassOf(Response.Either::class) -> buildEitherResponseMap()
            type.jvmErasure.isSubclassOf(Response.Listing::class) -> mapOf(
                200 to buildType()
            )
            type.jvmErasure.isSubclassOf(Response.Error::class) -> mapOf(
                422 to getErrorType()
            )
            type.jvmErasure.isSubclassOf(Response.Errors::class) -> mapOf(
                422 to buildType()
            )
            type.jvmErasure.isSubclassOf(Response.Data::class) -> mapOf(
                200 to buildType()
            )
            else -> mapOf(
                200 to type.type()
            )
        }
    }

    fun buildType(): Type.Object {
        return type.objectType(type.jvmErasure.simpleName!!)
    }

    private fun getErrorType(): Type {
        return Type.Object(
            "error",
            listOf(
                Property(
                    "error",
                    Type.String()
                )
            ),
            example = buildJsonObject { put("error", "Wrong response") }
        )
    }

    private fun getOkType(): Type {
        return Type.Object(
            "ok",
            listOf(
                Property(
                    "data",
                    Type.String()
                )
            ),
            example = buildJsonObject { put("data", "ok") }
        )
    }

    private fun buildEitherResponseMap(): Map<Int, Type> {
        val data = type.jvmErasure.memberProperties.first()
        val result = mutableMapOf<Int, Type>()
        data.returnType.arguments.forEach { argument ->
            var projectionType = type.getArgumentProjectionType(argument.type!!)
            when {
                projectionType.type.isSubtypeOf(typeOf<Response.Ok>()) -> {
                    result[200] = getOkType()
                }
                projectionType.type.isSubtypeOf(typeOf<Response.Errors>()) -> {
                    result[422] = projectionType.objectType("errors")
                }
                projectionType.type.isSubtypeOf(typeOf<Response.Error>()) -> {
                    result[422] = getErrorType()
                }
                else -> {
                    result[200] = projectionType.objectType(projectionType.jvmErasure.simpleName!!)
                }
            }
        }
        return result
    }
}
