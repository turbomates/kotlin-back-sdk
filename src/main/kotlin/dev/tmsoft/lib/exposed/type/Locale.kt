package dev.tmsoft.lib.exposed.type

import java.util.Locale
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi

/**
 * A column to store a locale.
 *
 * @param name The column name
 */
fun Table.locale(name: String): Column<Locale> = registerColumn(name, PostgreSQLLocale())

class PostgreSQLLocale : ColumnType() {
    override fun sqlType(): String = "LOCALE"

    override fun nonNullValueToString(value: Any): String {
        return when (value) {
            is String -> value
            is Locale -> value.toString()
            else -> error("Unexpected value: $value of ${value::class.qualifiedName}")
        }
    }

    override fun valueFromDB(value: Any): Any = when (value) {
        is Locale -> value
        is String -> Locale(value)
        else -> valueFromDB(value.toString())
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        stmt[index] = value.toString()
    }
}
