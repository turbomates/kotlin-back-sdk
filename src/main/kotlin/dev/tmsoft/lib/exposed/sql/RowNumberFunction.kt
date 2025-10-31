package dev.tmsoft.lib.exposed.sql

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Function
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.SortOrder

class RowNumberFunction(
    private val order: Array<Pair<Expression<*>, SortOrder>> = emptyArray(),
    private val partition: Array<Expression<*>> = emptyArray()
) : Function<Long>(LongColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder {
            +"ROW_NUMBER() OVER ("
            if (order.isNotEmpty()) {
                +"ORDER BY ${
                    order.joinToString(",") { QueryBuilder(true).append(it.first).toString() + " " + it.second.code }
                } "
            }
            if (partition.isNotEmpty()) {
                +" PARTITION BY ${partition.joinToString(",") { QueryBuilder(true).append(it).toString() }}"
            }
            +")"
        }
    }
}
