package dev.tmsoft.lib.exposed.timescale.sql.function

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Function
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.javatime.JavaLocalDateColumnType
import java.time.LocalDate

fun Column<*>.timeBucket(interval: String): TimeBucket = TimeBucket(this, interval)

class TimeBucket(private val expr: Expression<*>, private val interval: String) : Function<LocalDate>(JavaLocalDateColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        +"time_bucket('"
        +interval
        +"',"
        +expr
        +")"
    }
}
