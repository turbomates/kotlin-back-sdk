package dev.tmsoft.lib.exposed.type

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.ComparisonOp
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.IsNullOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.OrOp
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.QueryParameter
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.jdbc.JdbcConnectionImpl
import org.jetbrains.exposed.sql.transactions.TransactionManager

fun <T : Any> Table.array(name: String, columnType: ColumnType<T>): Column<Array<T>> =
    registerColumn(name, ArrayColumnType(columnType))

class ArrayColumnType<T : Any>(private val type: ColumnType<T>) : ColumnType<Array<T>>() {
    override fun sqlType(): String = buildString {
        append(type.sqlType())
        append("[]")
    }

    override fun valueToDB(value: Array<T>?): Any? {
        if (value is Array<*>) {
            val columnType = type.sqlType().split("(")[0]
            val jdbcConnection = (TransactionManager.current().connection as JdbcConnectionImpl).connection
            return jdbcConnection.createArrayOf(columnType, value)
        } else {
            return super.valueToDB(value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun valueFromDB(value: Any): Array<T> {
        if (value is java.sql.Array) {
            return value.array as Array<T>
        }
        if (value is Array<*>) {
            return value as Array<T>
        }
        error("Array does not support for this database")
    }

    override fun notNullValueToDB(value: Array<T>): Any {
        if (value.isEmpty()) return "'{}'"

        val columnType = type.sqlType().split("(")[0]
        val jdbcConnection = (TransactionManager.current().connection as JdbcConnectionImpl).connection
        return jdbcConnection.createArrayOf(columnType, value) ?: error("Can't create non null array for $value")
    }
}

class AnyOp(private val expr1: Expression<*>, private val expr2: Expression<*>) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        if (expr2 is OrOp) {
            queryBuilder.append("(").append(expr2).append(")")
        } else {
            queryBuilder.append(expr2)
        }
        queryBuilder.append(" = ANY (")
        if (expr1 is OrOp) {
            queryBuilder.append("(").append(expr1).append(")")
        } else {
            queryBuilder.append(expr1)
        }
        queryBuilder.append(")")
    }
}

class ContainsOp(expr1: Expression<*>, expr2: Expression<*>) : ComparisonOp(expr1, expr2, "@>")

infix fun <T> ExpressionWithColumnType<Array<T>>.any(t: Array<T>?): Op<Boolean> {
    if (t == null) {
        return IsNullOp(this)
    }
    return AnyOp(this, QueryParameter(t, columnType))
}

infix fun <T> ExpressionWithColumnType<Array<T>>.contains(array: Array<T>): Op<Boolean> =
    ContainsOp(this, QueryParameter(array, columnType))
