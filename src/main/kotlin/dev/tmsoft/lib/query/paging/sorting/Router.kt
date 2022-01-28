package dev.tmsoft.lib.query.paging.sorting

import dev.tmsoft.lib.openapi.OpenAPI
import dev.tmsoft.lib.openapi.Property
import dev.tmsoft.lib.openapi.Type
import dev.tmsoft.lib.openapi.ktor.buildFullPath
import dev.tmsoft.lib.openapi.ktor.openApi
import dev.tmsoft.lib.query.PathValues
import dev.tmsoft.lib.query.SingleValue
import dev.tmsoft.lib.query.Value
import dev.tmsoft.lib.query.exceptions.InvalidValue
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
                "$sortingParameterName[${field.name}]", Type.String(field.values.ifEmpty { null })
            )
        )
    }
    return Type.Object("sorting", parameters)
}

fun Parameters.sortingValues(): PathValues {
    val parameters = mutableMapOf<String, List<Value>>()
    forEach { key, values ->
        if (key.contains(sortingParameterName)) {
            val singleValue = values.singleOrNull()
                ?.let { value -> SingleValue(value) }
                ?: throw InvalidValue("Unknown values. Should be a single", values)

            parameters[singleValue.value] = listOf(singleValue)
        }
    }
    return PathValues(parameters)
}

private val sortingParameterName: String
    get() = "sorting"
