package dev.tmsoft.lib.query.filter

import dev.tmsoft.lib.query.exceptions.NotEnoughInformation
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnSet
import org.jetbrains.exposed.sql.EnumerationNameColumnType
import org.jetbrains.exposed.sql.OrOp
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Slice
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.andWhere

abstract class Filter(val table: Table) {
    private val fields: MutableList<Field> = mutableListOf()

    fun fields(): List<Field> {
        return fields
    }

    fun add(
        name: String,
        column: Column<*>,
        possibleValues: List<String>? = null,
        function: (Query.(values: List<Value>) -> Query)? = null
    ): Field {
        val field = Field(
            name,
            function ?: { values ->
                andWhere {
                    if (values.size > 1) {
                        OrOp(values.map { it.op(column) })
                    } else {
                        values.first().op(column)
                    }
                }
            },
            possibleValues ?: column.possibleValues()
        )

        fields.add(field)
        return field
    }

    fun add(
        name: String,
        possibleValues: List<String>? = null,
        function: (Query.(value: List<Value>) -> Query)? = null
    ): Field {
        val column = table.columns.find { it.name == name }
        val field = if (column != null) {
            column.field(name, function)
        } else {
            if (function == null) {
                throw NotEnoughInformation("Please provide function and conditions")
            }
            Field(name, function, possibleValues ?: emptyList())
        }

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

    private fun Column<*>.field(
        name: String,
        customFunction: (Query.(value: List<Value>) -> Query)? = null
    ): Field {
        return if (customFunction == null) {
            Field(
                name,
                { values ->
                    andWhere {
                        if (values.size == 1) {
                            values.first().op(this@field)
                        } else {
                            OrOp(values.map { it.op(this@field) })
                        }
                    }
                },
                possibleValues()
            )
        } else {
            Field(name, customFunction, possibleValues())
        }
    }

    private fun Column<*>.possibleValues(): List<String> {
        return when (columnType) {
            is EnumerationNameColumnType<*> -> (columnType as EnumerationNameColumnType<*>).klass.java.enumConstants.map { it.name }
            else -> emptyList()
        }
    }
}

fun Query.filter(filter: Filter, values: PathValues): Query {
    return filter.apply(this, values)
}

fun Query.addJoin(body: ColumnSet.() -> ColumnSet): Query {
    return adjustSlice { old ->
        val newFieldSet = body()
        if (set.source.columns.containsAll(newFieldSet.source.columns)) {
            old
        } else {
            Slice(newFieldSet.source, old.fields)
        }
    }
}
