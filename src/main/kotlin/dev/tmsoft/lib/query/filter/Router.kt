package dev.tmsoft.lib.query.filter

import com.turbomates.openapi.Property
import com.turbomates.openapi.Type
import com.turbomates.openapi.ktor.buildFullPath
import com.turbomates.openapi.ktor.openApi
import io.ktor.http.Parameters
import io.ktor.server.routing.Route

fun Route.filterDescription(filter: Filter): Route {
    openApi.extendDocumentation { _ ->
        addToPath(
            buildFullPath(),
            com.turbomates.openapi.OpenAPI.Method.GET,
            queryParams = filter.openApiType()
        )
    }
    return this
}

private fun Filter.openApiType(): Type.Object {
    val parameters = mutableListOf<Property>()
    fields().forEach { field ->
        parameters.add(
            Property("$filterParameterName[${field.name}]", Type.String(field.values.ifEmpty { null }))
        )
    }
    return Type.Object("filters", parameters, nullable = true)
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
