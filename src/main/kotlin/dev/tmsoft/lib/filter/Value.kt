package dev.tmsoft.lib.filter

import dev.tmsoft.lib.date.localDateFormat
import dev.tmsoft.lib.date.localDateTimeFormat
import org.jetbrains.exposed.sql.*
import java.time.LocalDate
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.wrap
import org.jetbrains.exposed.sql.javatime.JavaLocalDateColumnType
import org.jetbrains.exposed.sql.javatime.JavaLocalDateTimeColumnType
import java.lang.UnsupportedOperationException

abstract class Value {
    abstract fun op(column: ExpressionWithColumnType<*>): Op<Boolean>
    protected fun ExpressionWithColumnType<*>.typedWrap(value: String): QueryParameter<*> {
        val typedValue = when (columnType) {
            is LongColumnType -> value.toLong()
            is IntegerColumnType -> value.toInt()
            is DoubleColumnType -> value.toDouble()
            is BooleanColumnType -> value.toBoolean()
            is JavaLocalDateColumnType -> LocalDate.parse(value, localDateFormat)
            is JavaLocalDateTimeColumnType -> LocalDateTime.parse(value, localDateTimeFormat)
            else -> value
        }
        return QueryParameter(typedValue, columnType)
    }
}

class SingleValue(val value: String) : Value() {
    override fun op(column: ExpressionWithColumnType<*>): Op<Boolean> {
        return column.expression(value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ExpressionWithColumnType<*>.expression(value: String): Op<Boolean> {
        return when (columnType) {
            is EnumerationColumnType<*> -> EqOp(this, typedWrap(value))
            is EnumerationNameColumnType<*> -> EqOp(this, typedWrap(value))
            is StringColumnType -> LikeOp(
                (this as ExpressionWithColumnType<String>).lowerCase(),
                wrap(value.lowercase() + "%")
            )
            else -> EqOp(this, typedWrap(value))
        }
    }
}

class RangeValue(val from: String? = null, val to: String? = null) : Value() {
    private val containsValue: String
        get() = from?.let { it.lowercase() + '%' } ?: '%' + to!!.lowercase()

    override fun op(column: ExpressionWithColumnType<*>): Op<Boolean> {
        return column.expression(from, to)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ExpressionWithColumnType<*>.expression(from: String?, to: String?): Op<Boolean> {
        return when (columnType) {
            is StringColumnType -> LikeOp(
                (this as ExpressionWithColumnType<String>).lowerCase(),
                wrap(containsValue)
            )
            else -> {
                val fromExpr = from?.let { GreaterEqOp(this, typedWrap(it)) }
                val toExpr = to?.let { LessEqOp(this, typedWrap(it)) }
                return if (fromExpr != null && toExpr != null) {
                    AndOp(
                        listOf(
                            fromExpr,
                            toExpr
                        )
                    )
                } else {
                    fromExpr ?: toExpr!!
                }
            }
        }
    }
}

class ListValue(val values: List<Value>) : Value() {
    override fun op(column: ExpressionWithColumnType<*>): Op<Boolean> {
        return OrOp(values.map { it.op(column) })
    }
}

class MapValue(val value: Map<String, Value>) : Value() {
    override fun op(column: ExpressionWithColumnType<*>): Op<Boolean> {
        throw UnsupportedOperationException("$this is not supported operations with columns")
    }
}
