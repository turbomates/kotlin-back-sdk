package dev.tmsoft.lib.exposed.type

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

/**
 * A column to store a time.
 *
 * @param name The column name
 */
fun Table.time(name: String): Column<LocalTime> = registerColumn(name, PostgreSQLTime())

class PostgreSQLTime : ColumnType<LocalTime>() {
    override fun sqlType(): String = "TIME"

    override fun nonNullValueToString(value: LocalTime): String {
        return value.format(DateTimeFormatter.ISO_TIME)
    }

    override fun valueFromDB(value: Any): LocalTime? = when (value) {
        is LocalTime -> value
        is String -> LocalTime.parse(value, DateTimeFormatter.ISO_TIME)
        else -> valueFromDB(value.toString())
    }
}
