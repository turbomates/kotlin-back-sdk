package dev.tmsoft.lib.exposed.timescale.sql.function

import com.turbomates.time.exposed.UTCDateTimeColumn
import java.time.OffsetDateTime
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Function
import org.jetbrains.exposed.v1.core.QueryBuilder

fun Column<*>.timeBucket(interval: String): TimeBucket = TimeBucket(this, interval)

class TimeBucket(private val expr: Expression<*>, private val interval: String) : Function<OffsetDateTime>(
    UTCDateTimeColumn()
) {
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
