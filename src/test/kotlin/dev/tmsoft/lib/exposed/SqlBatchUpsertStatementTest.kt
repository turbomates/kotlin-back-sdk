package dev.tmsoft.lib.exposed

import java.util.UUID
import kotlin.test.assertEquals
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test

class SqlBatchUpsertStatementTest {
    @Test
    fun `batch upsert`() {
        transaction(testDatabase) {
            SchemaUtils.create(Accounts)
            val testData = (0..299).map {
                Account("test $it", it, UUID.randomUUID())
            }

            Accounts.singleSQLBatchInsert(testData) {
                this[Accounts.name] = it.name
                this[Accounts.balance] = it.balance
                this[Accounts.reference] = it.reference
            }

            val upsertData = testData.mapIndexed { index, account ->
                if (index < 150)
                    Account("upserted", index, account.reference)
                else
                    Account("inserted", index, UUID.randomUUID())
            }

            Accounts.singleSqlBatchUpsert(upsertData, Accounts.reference) { statement ->
                this[Accounts.name] = statement.name
                this[Accounts.balance] = statement.balance
                this[Accounts.reference] = statement.reference
            }

            assertEquals(450, Accounts.selectAll().count())
            assertEquals("upserted", Accounts.selectAll().orderBy(Accounts.id, SortOrder.ASC).limit(1).single().let { it[Accounts.name] })
            SchemaUtils.drop(Accounts)
        }
    }

    data class Account(val name: String, val balance: Int, val reference: UUID)
    object Accounts : IntIdTable("test") {
        val name = varchar("account", 255).nullable()
        val balance = integer("balance")
        val reference = uuid("reference").uniqueIndex("uniq_reference")
    }
}
