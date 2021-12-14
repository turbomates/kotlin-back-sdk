package dev.tmsoft.lib.filter

import dev.tmsoft.lib.openapi.OpenAPI
import dev.tmsoft.lib.openapi.Property
import dev.tmsoft.lib.openapi.Type
import dev.tmsoft.lib.openapi.ktor.buildFullPath
import dev.tmsoft.lib.openapi.ktor.openApi
import io.ktor.http.Parameters
import io.ktor.routing.Route

fun Route.filterDescription(filter: Filter): Route {
    openApi.addToPath(
        buildFullPath(),
        OpenAPI.Method.GET,
        pathParams = filter.openApiType()
    )
    return this
}

private fun Filter.openApiType(): Type.Object {
    val parameters = mutableListOf<Property>()
    fields().forEach { field ->
        parameters.add(
            Property(
                "filter[${field.name}]", Type.String(if (field.values.isNotEmpty()) field.values else null)
            )
        )
    }
    return Type.Object("filters", parameters)
}

fun Parameters.filterValues(): PathValues {
    val parameters = mutableMapOf<String, List<Value>>()
    forEach { key, value ->
        if (key.contains(parameterName)) {
            val result = Regex("\\[(\\w+)\\]").findAll(key)
            val field: String = result.last().groupValues.last()
            parameters[field] = value.convert()
        }
    }
    return PathValues(parameters)
}

private fun List<String>.convert(): List<Value> = map { it.convert() }

private fun String.convert(): Value {
    return when {
        this.first() == '{' && this.last() == '}' -> {
            val mapValue = this.substring(1, this.length - 1).split(",").associate { it.split(":").let { (k, v) -> k to v.convert() } }
            MapValue(mapValue)
        }
        this.first() == '[' && this.last() == ']' ->
            ListValue(this.substring(1, this.length - 1).split(",").map { it.convert() })
        this.contains("~") ->
            RangeValue(this.split("~").first().filterValue(), this.split("~")[1].filterValue())
        else ->
            SingleValue(this)
    }
}

private fun String.filterValue(): String? {
    return if (isBlank()) {
        null
    } else {
        this
    }
}

private val parameterName: String
    get() = "filter"
