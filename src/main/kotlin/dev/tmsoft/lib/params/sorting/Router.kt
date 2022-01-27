package dev.tmsoft.lib.params.sorting

import dev.tmsoft.lib.openapi.OpenAPI
import dev.tmsoft.lib.openapi.Property
import dev.tmsoft.lib.openapi.Type
import dev.tmsoft.lib.openapi.ktor.buildFullPath
import dev.tmsoft.lib.openapi.ktor.openApi
import dev.tmsoft.lib.params.PathValues
import dev.tmsoft.lib.params.QueryConverter
import dev.tmsoft.lib.params.SingleValue
import dev.tmsoft.lib.params.Value
import dev.tmsoft.lib.params.exceptions.InvalidValue
import io.ktor.http.Parameters
import io.ktor.routing.Route

fun Route.sortingDescription(sorting: Sorting): Route {
    openApi.addToPath(
        buildFullPath(),
        OpenAPI.Method.GET,
        pathParams = sorting.openApiType()
    )
    return this
}

private fun Sorting.openApiType(): Type.Object {
    val parameters = mutableListOf<Property>()
    fields().forEach { field ->
        parameters.add(
            Property(
                "sorting[${field.name}]", Type.String(field.values.ifEmpty { null })
            )
        )
    }
    return Type.Object("sorting", parameters)
}

fun Parameters.sortingValues(): PathValues {
    val parameters = mutableMapOf<String, List<Value>>()
    forEach { key, values ->
        if (key.contains("sorting")) {
            val singleValue = QueryConverter.convert(values)
                .singleOrNull { value -> value is SingleValue } as SingleValue?
                ?: throw InvalidValue("Unknown values. Should be a single", values)

            parameters[singleValue.value] = listOf(singleValue)
        }
    }
    return PathValues(parameters)
}
