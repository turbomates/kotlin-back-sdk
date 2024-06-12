package dev.tmsoft.lib.exposed.timescale.sql.function

import com.turbomates.time.exposed.UTCDateTimeColumn
import java.time.OffsetDateTime
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Function
import org.jetbrains.exposed.sql.QueryBuilder

fun Column<*>.timeBucket(interval: String): TimeBucket = TimeBucket(this, interval)

class TimeBucket(private val expr: Expression<*>, private val interval: String) : Function<OffsetDateTime>(UTCDateTimeColumn()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder {
            +"time_bucket('"
            +interval
            +"',"
            +expr
            +")"
        }
    }
}
