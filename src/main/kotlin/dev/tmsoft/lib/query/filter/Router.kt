package dev.tmsoft.lib.query.filter

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
                "$filterParameterName[${field.name}]", Type.String(field.values.ifEmpty { null })
            )
        )
    }
    return Type.Object("filters", parameters)
}

fun Parameters.filterValues(): PathValues {
    val parameters = mutableMapOf<String, List<Value>>()
    forEach { key, value ->
        if (key.contains(filterParameterName)) {
            val result = Regex("\\[(\\w+)\\]").findAll(key)
            val field: String = result.last().groupValues.last()
            parameters[field] = QueryConverter.convert(value)
        }
    }
    return PathValues(parameters)
}

private val filterParameterName: String
    get() = "filter"
