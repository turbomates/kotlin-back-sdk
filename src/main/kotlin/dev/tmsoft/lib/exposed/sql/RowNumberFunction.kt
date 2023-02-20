package dev.tmsoft.lib.exposed.sql

import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Function
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.SortOrder

class RowNumberFunction(
    private val order: List<Pair<Expression<*>, SortOrder>> = emptyList(),
    val partition: List<Expression<*>> = emptyList()
) : Function<Long>(LongColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        +"ROW_NUMBER() OVER ("
        +"ORDER BY ${order.map { QueryBuilder(true).append(it.first).toString() + " " + it.second.code }.joinToString(",")}"
        +")"
    }
}
