package dev.tmsoft.lib.query.filter

import com.turbomates.time.dateFormat
import com.turbomates.time.dateTimeFormat
import com.turbomates.time.exposed.UTCDateTimeColumn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import org.jetbrains.exposed.sql.AndOp
import org.jetbrains.exposed.sql.BooleanColumnType
import org.jetbrains.exposed.sql.DoubleColumnType
import org.jetbrains.exposed.sql.EqOp
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.GreaterEqOp
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.LessEqOp
import org.jetbrains.exposed.sql.LikeEscapeOp
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.OrOp
import org.jetbrains.exposed.sql.QueryParameter
import org.jetbrains.exposed.sql.SqlExpressionBuilder.wrap
import org.jetbrains.exposed.sql.StringColumnType
import org.jetbrains.exposed.sql.javatime.JavaLocalDateColumnType
import org.jetbrains.exposed.sql.javatime.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.sql.lowerCase

abstract class Value {
    abstract fun op(column: ExpressionWithColumnType<*>): Op<Boolean>
    @Suppress("UNCHECKED_CAST")
    protected fun <T> ExpressionWithColumnType<T>.typedWrap(value: String): QueryParameter<T> {
        val typedValue = when (columnType) {
            is LongColumnType -> value.toLong()
            is IntegerColumnType -> value.toInt()
            is DoubleColumnType -> value.toDouble()
            is BooleanColumnType -> value.toBoolean()
            is JavaLocalDateColumnType -> LocalDate.parse(value, dateFormat)
            is JavaLocalDateTimeColumnType -> LocalDateTime.parse(value, dateTimeFormat)
            is UTCDateTimeColumn -> OffsetDateTime.parse(value, dateTimeFormat)
            else -> value
        }
        return QueryParameter(typedValue as T, columnType)
    }
}

class SingleValue(val value: String) : Value() {
    override fun op(column: ExpressionWithColumnType<*>): Op<Boolean> {
        return column.expression(value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ExpressionWithColumnType<*>.expression(value: String): Op<Boolean> {
        return if (columnType is StringColumnType) {
            LikeEscapeOp(
                (this as ExpressionWithColumnType<String>).lowerCase(),
                wrap(value.lowercase() + "%"),
                true,
                null
            )
        } else EqOp(this, typedWrap(value))
    }
}

class RangeValue(val from: String? = null, val to: String? = null) : Value() {
    private val containsValue: String
        get() = from?.let { it.lowercase() + '%' } ?: ('%' + to!!.lowercase())

    override fun op(column: ExpressionWithColumnType<*>): Op<Boolean> {
        return column.expression(from, to)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ExpressionWithColumnType<*>.expression(from: String?, to: String?): Op<Boolean> {
        return if (columnType is StringColumnType) {
            LikeEscapeOp(
                (this as ExpressionWithColumnType<String>).lowerCase(),
                wrap(containsValue),
                true,
                null
            )
        } else {
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
