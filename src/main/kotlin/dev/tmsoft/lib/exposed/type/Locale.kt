package dev.tmsoft.lib.exposed.type

import java.util.Locale
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi

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
        stmt.set(index, value?.run { toString() } ?: "", this)
    }
}
