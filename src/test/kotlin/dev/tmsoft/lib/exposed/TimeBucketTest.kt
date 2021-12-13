package dev.tmsoft.lib.exposed

import dev.tmsoft.lib.Config
import dev.tmsoft.lib.exposed.timescale.sql.function.timeBucket
import java.util.UUID
import kotlin.test.assertEquals
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertTrue

class TimeBucketTest {
    @Test
    fun `query group by time bucket`() {
        val database = Database.connect(
            Config.h2DatabaseUrl,
            driver = Config.h2Driver,
            user = Config.h2User,
            password = Config.h2Password
        )
        transaction(database) {
            SchemaUtils.create(Accounts)
            (1..5).forEach { index ->
                Accounts.insert {
                    it[name] = "test $index"
                    it[balance] = index
                    it[reference] = UUID.randomUUID()
                    it[createdAt] = LocalDateTime.of(2021, 1, 1, index, 0)
                }
            }
            val createdAtTimeBucket = Accounts.createdAt.timeBucket("2 days").alias("data")

            val query = Accounts.slice(createdAtTimeBucket, Accounts.balance.sum())
                .selectAll()
                .groupBy(createdAtTimeBucket)

            assertTrue(query.prepareSQL(QueryBuilder(true)).contains("time_bucket"))
            // assertEquals(3, query.count())
        }
    }

    object Accounts : IntIdTable("test") {
        val name = varchar("name", 255).nullable()
        val balance = integer("balance")
        val createdAt = datetime("created_at").default(LocalDateTime.now())
        val reference = uuid("reference").uniqueIndex("uniq_referenec")
    }
}
