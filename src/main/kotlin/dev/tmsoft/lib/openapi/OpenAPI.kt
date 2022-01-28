package dev.tmsoft.lib.openapi

import dev.tmsoft.lib.openapi.spec.InfoObject
import dev.tmsoft.lib.openapi.spec.MediaTypeObject
import dev.tmsoft.lib.openapi.spec.OperationObject
import dev.tmsoft.lib.openapi.spec.ParameterObject
import dev.tmsoft.lib.openapi.spec.PathItemObject
import dev.tmsoft.lib.openapi.spec.RequestBodyObject
import dev.tmsoft.lib.openapi.spec.ResponseObject
import dev.tmsoft.lib.openapi.spec.Root
import dev.tmsoft.lib.openapi.spec.SchemaObject
import kotlin.reflect.KClass
import kotlinx.serialization.json.JsonElement

class OpenAPI(var host: String) {
    val root: Root = Root("3.0.2", InfoObject("Api", version = "0.1.0"))
    private val customTypes: MutableMap<String, Type> = mutableMapOf()

    fun addToPath(
        path: String,
        method: Method,
        responses: Map<Int, Type> = emptyMap(),
        body: Type.Object? = null,
        pathParams: Type.Object? = null
    ) {
        var pathItemObject = root.paths[path]
        if (pathItemObject == null) {
            pathItemObject = PathItemObject()
            root.paths[path] = pathItemObject
        }
        val updatedPathParams = responses.values
            .find { value -> value is Type.Object && value.name == "Listing" }
            ?.let {
                Type.Object(
                    "paging",
                    listOf(
                        Property("pageSize", Type.Number),
                        Property("page", Type.Number)
                    ).plus(pathParams?.properties ?: emptyList())
                )
            }

        when (method) {
            Method.GET -> {
                pathItemObject.get = pathItemObject.get?.merge(pathParams, body, responses) ?: OperationObject(
                    responses.mapValues { it.value.toResponseObject() },
                    parameters = updatedPathParams?.toParameterObject()
                )
            }
            Method.POST -> {
                pathItemObject.post = OperationObject(
                    responses.mapValues { it.value.toResponseObject() },
                    requestBody = body?.toRequestBodyObject(),
                    parameters = updatedPathParams?.toParameterObject()
                )
            }
            Method.DELETE -> {
                pathItemObject.delete = OperationObject(
                    responses.mapValues { it.value.toResponseObject() },
                    requestBody = body?.toRequestBodyObject(),
                    parameters = updatedPathParams?.toParameterObject()
                )
            }
            Method.PATCH -> {
                pathItemObject.patch = OperationObject(
                    responses.mapValues { it.value.toResponseObject() },
                    requestBody = body?.toRequestBodyObject(),
                    parameters = updatedPathParams?.toParameterObject()
                )
            }
        }
    }

    fun setCustomClassType(clazz: KClass<*>, type: Type) {
        customTypes[clazz.qualifiedName!!] = type
    }

    private fun Type.toResponseObject(): ResponseObject {
        return ResponseObject(
            "empty description",
            content = mapOf("application/json" to MediaTypeObject(schema = toSchemaObject()))
        )
    }

    private fun Type.toRequestBodyObject(): RequestBodyObject {
        return RequestBodyObject(
            content = mapOf("application/json" to MediaTypeObject(schema = toSchemaObject()))
        )
    }

    private fun Type.Object.toParameterObject(): List<ParameterObject> {
        return properties.map {
            ParameterObject(it.name, schema = it.type.toSchemaObject(), `in` = "path")
        }
    }

    private fun Type.toSchemaObject(): SchemaObject {
        return when (this) {
            is Type.String -> {
                SchemaObject(type = "string", enum = this.values, example = this.example)
            }
            is Type.Array -> {
                SchemaObject(type = "array", items = this.type.toSchemaObject(), enum = this.values)
            }
            is Type.Object -> {
                if (customTypes.containsKey(this.returnType) && this.returnType !== null) {
                    customTypes.getValue(this.returnType).toSchemaObject()
                } else {
                    SchemaObject(
                        type = "object",
                        properties = this.properties.associate { it.name to it.type.toSchemaObject() },
                        example = this.example
                    )
                }
            }
            is Type.Boolean -> {
                SchemaObject(type = "boolean")
            }
            is Type.Number -> {
                SchemaObject(type = "number")
            }
        }
    }

    enum class Method {
        GET, POST, DELETE, PATCH
    }

    private fun OperationObject.merge(
        pathParams: Type.Object? = null,
        body: Type.Object? = null,
        responses: Map<Int, Type>
    ): OperationObject {
        val parameters: List<ParameterObject>? =
            (
                    parameters?.plus(pathParams?.toParameterObject() ?: emptyList())
                        ?: pathParams?.toParameterObject()
                    )
        val bodyResult = body?.toRequestBodyObject() ?: this.requestBody
        val responsesResult = this.responses + responses.mapValues { it.value.toResponseObject() }
        return copy(parameters = parameters, requestBody = bodyResult, responses = responsesResult)
    }
}

data class Property(
    val name: String,
    val type: Type
)

sealed class Type {
    class String(val values: List<kotlin.String>? = null, val example: JsonElement? = null) : Type()
    class Array(val type: Type, val values: List<kotlin.String>? = null) : Type()
    data class Object(
        val name: kotlin.String,
        val properties: List<Property>,
        val example: JsonElement? = null,
        val returnType: kotlin.String? = null
    ) : Type()

    object Boolean : Type()
    object Number : Type()
}
