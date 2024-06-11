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

class PostgreSQLLocale : ColumnType<Locale>() {
    override fun sqlType(): String = "LOCALE"

    override fun nonNullValueToString(value: Locale): String {
        return value.toString()
    }

    override fun valueFromDB(value: Any): Locale? = when (value) {
        is Locale -> value
        is String -> Locale.of(value)
        else -> valueFromDB(value.toString())
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        stmt[index] = value?.run { toString() } ?: ""
    }
}
