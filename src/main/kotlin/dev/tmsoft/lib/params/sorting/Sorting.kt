package dev.tmsoft.lib.params.sorting

import dev.tmsoft.lib.params.Field
import dev.tmsoft.lib.params.PathValues
import dev.tmsoft.lib.params.SingleValue
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.EnumerationNameColumnType
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table

abstract class Sorting(val table: Table) {
    private val fields: MutableList<Field> = mutableListOf()

    fun fields(): List<Field> = fields

    fun add(
        name: String,
        column: Column<*>? = null,
        possibleValues: List<String>? = null
    ): Field {
        val tableColumn = column ?: table.columns.first { it.name == name }

        val field = Field(
            name,
            { values ->
                val sortOrder = (values.single() as SingleValue).value
                    .replace(" ","")
                    .uppercase()
                orderBy(
                    tableColumn,
                    SortOrder.valueOf(sortOrder)
                )
            },
            possibleValues ?: tableColumn.possibleValues()
        )

        fields.add(field)
        return field
    }

    fun apply(query: Query, parameters: PathValues): Query {
        var buildQuery = query.copy()
        fields.forEach { field ->
            val values = parameters[field.name] ?: emptyList()
            if (values.isNotEmpty()) {
                buildQuery = field.function(buildQuery, values)
            }
        }
        return buildQuery
    }

    private fun Column<*>.possibleValues(): List<String> {
        return when (columnType) {
            is EnumerationNameColumnType<*> -> (columnType as EnumerationNameColumnType<*>).klass.java.enumConstants.map { it.name }
            else -> emptyList()
        }
    }
}

fun Query.sortedBy(sorting: Sorting, values: PathValues): Query {
    return sorting.apply(this, values)
}
