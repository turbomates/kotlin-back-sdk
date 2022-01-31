package dev.tmsoft.lib.query.paging

import dev.tmsoft.lib.query.exceptions.InvalidValue
import io.ktor.http.Parameters
import org.jetbrains.exposed.sql.SortOrder

fun Parameters.sortingParameters(): List<SortingParameter> {
    val parameters = mutableListOf<SortingParameter>()
    forEach { key, values ->
        if (key.contains(sortingParameterName)) {
            values.singleOrNull()
                ?.let { value -> parameters.add(SortingParameter(value, SortOrder.valueOf(value))) }
                ?: throw InvalidValue("Unknown values. Should be a single value", values)
        }
    }
    return parameters
}

private val sortingParameterName: String
    get() = "sorting"

data class SortingParameter(
    val name: String,
    val sortOrder: SortOrder
)
