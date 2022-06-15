package dev.tmsoft.lib.query.filter.type

import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.QueryParameter
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.javatime.JavaLocalDateColumnType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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
    return if (expression.columnType is JavaLocalDateColumnType) {
        QueryParameter<LocalDate>(toLocalDate(), expression.columnType)
    } else QueryParameter(this, expression.columnType)
}
