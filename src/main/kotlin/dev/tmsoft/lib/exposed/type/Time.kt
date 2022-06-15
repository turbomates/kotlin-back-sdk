package dev.tmsoft.lib.exposed.type

import java.time.LocalTime
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import java.time.format.DateTimeFormatter

/**
 * A column to store a time.
 *
 * @param name The column name
 */
fun Table.time(name: String): Column<LocalTime> = registerColumn(name, PostgreSQLTime())

class PostgreSQLTime : ColumnType() {
    override fun sqlType(): String = "TIME"

    override fun nonNullValueToString(value: Any): String {
        return when (value) {
            is String -> return value
            is LocalTime -> value.format(DateTimeFormatter.ISO_TIME)
            else -> error("Unexpected value: $value of ${value::class.qualifiedName ?: "time"}")
        }
    }

    override fun valueFromDB(value: Any): Any = when (value) {
        is LocalTime -> value
        is String -> LocalTime.parse(value, DateTimeFormatter.ISO_TIME)
        else -> valueFromDB(value.toString())
    }
}
