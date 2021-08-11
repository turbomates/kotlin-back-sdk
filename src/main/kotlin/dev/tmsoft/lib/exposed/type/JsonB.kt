package dev.tmsoft.lib.exposed.type

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnSet
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.ComparisonOp
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.append
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject

fun <T : Any> Table.jsonb(
    name: String,
    serializer: KSerializer<T>,
    module: SerializersModule = EmptySerializersModule
): Column<T> {
    return registerColumn(name, PostgreSQLJson(serializer, module))
}

class PostgreSQLJson<out T : Any>(
    private val serializer: KSerializer<T>,
    module: SerializersModule = EmptySerializersModule
) : ColumnType() {
    private val json = Json {
        serializersModule = module
    }

    override fun sqlType() = "jsonb"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val obj = PGobject()
        obj.type = "jsonb"
        obj.value = value as String
        stmt[index] = obj
    }

    override fun notNullValueToDB(value: Any): Any {
        return nonNullValueToString(value)
    }

    override fun valueFromDB(value: Any): Any {
        if (value is PGobject) {
            return json.decodeFromString(serializer, value.value!!)
        }
        return value
    }

    @Suppress("UNCHECKED_CAST")
    override fun nonNullValueToString(value: Any): String {
        return json.encodeToString(serializer, value as T)
    }

    override fun valueToString(value: Any?): String = when (value) {
        null -> {
            if (!nullable) error("NULL in non-nullable column")
            "NULL"
        }
        else -> {
            nonNullValueToString(value)
        }
    }
}

class JsonBContains(expr1: Expression<*>, expr2: Expression<*>) : ComparisonOp(expr1, expr2, "@>")

class JsonBArray<E : Any, T : List<E>>(private val column: Column<T>, serializer: KSerializer<E>) :
    Table("jsonbarray") {
    val value: Column<E> = registerColumn("value", PostgreSQLJson(serializer))
    override val columns: List<Column<*>> = emptyList()

    override fun describe(s: Transaction, queryBuilder: QueryBuilder) {
        queryBuilder { append("jsonb_array_elements(", column, ") jsonbarray") }
    }

    override infix fun innerJoin(otherTable: ColumnSet): Join = Join(this, otherTable, JoinType.INNER)
    override fun join(
        otherTable: ColumnSet,
        joinType: JoinType,
        onColumn: Expression<*>?,
        otherColumn: Expression<*>?,
        additionalConstraint: (SqlExpressionBuilder.() -> Op<Boolean>)?
    ): Join = Join(this, otherTable, joinType, onColumn, otherColumn, additionalConstraint)

    override infix fun leftJoin(otherTable: ColumnSet): Join = Join(this, otherTable, JoinType.LEFT)

    override infix fun rightJoin(otherTable: ColumnSet): Join = Join(this, otherTable, JoinType.RIGHT)

    override infix fun fullJoin(otherTable: ColumnSet): Join = Join(this, otherTable, JoinType.FULL)

    override infix fun crossJoin(otherTable: ColumnSet): Join = Join(this, otherTable, JoinType.CROSS)
}
