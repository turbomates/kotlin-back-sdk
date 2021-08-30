package dev.tmsoft.lib.filter.type

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.QueryParameter
import org.jetbrains.exposed.sql.`java-time`.JavaLocalDateColumnType
import org.jetbrains.exposed.sql.andWhere

data class DateTimeRange(val from: LocalDateTime? = null, val to: LocalDateTime? = null)

fun Query.andDateRange(range: DateTimeRange, expression: ExpressionWithColumnType<*>): Query {
    return apply {
        range.from?.let { andWhere { expression greaterEq it.queryValue(expression) } }
        range.to?.let {
            andWhere {
                expression lessEq LocalDateTime.of(it.toLocalDate(), LocalTime.MAX).queryValue(expression)
            }
        }
    }
}

fun LocalDateTime.queryValue(expression: ExpressionWithColumnType<*>): QueryParameter<*> {
    return when (expression.columnType) {
        is JavaLocalDateColumnType -> QueryParameter<LocalDate>(toLocalDate(), expression.columnType)
        else -> QueryParameter(this, expression.columnType)
    }
}
