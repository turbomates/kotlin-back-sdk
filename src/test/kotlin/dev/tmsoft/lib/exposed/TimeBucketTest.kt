package dev.tmsoft.lib.exposed

import com.turbomates.time.exposed.CurrentTimestamp
import com.turbomates.time.exposed.datetime
import dev.tmsoft.lib.exposed.timescale.sql.function.timeBucket
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals

class TimeBucketTest {
    @Test
    fun `query group by time bucket`() {
        transaction(testDatabase) {
            SchemaUtils.create(Accounts)

            exec("CREATE EXTENSION IF NOT EXISTS timescaledb;")
            exec("SELECT create_hypertable('${Accounts.tableName}', 'created_at');")

            (1..5).forEach { index ->
                Accounts.insert {
                    it[name] = "test $index"
                    it[balance] = index
                    it[createdAt] = OffsetDateTime.of(2021, 1, index, 12, 0, 0, 0, ZoneOffset.UTC)
                }
            }
            val createdAtTimeBucket = Accounts.createdAt.timeBucket("2 days").alias("data")

            val query = Accounts.select(createdAtTimeBucket, Accounts.balance.sum())
                .groupBy(createdAtTimeBucket)

            assertEquals(3, query.count())
            SchemaUtils.drop(Accounts)
        }
    }

    object Accounts : Table("test") {
        val name = varchar("name", 255).nullable()
        val balance = integer("balance")
        val createdAt = datetime("created_at").defaultExpression(CurrentTimestamp())
    }
}
