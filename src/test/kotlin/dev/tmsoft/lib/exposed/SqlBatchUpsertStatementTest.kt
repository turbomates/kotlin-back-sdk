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

    @Test
    fun `batch upsert with account`() {
        transaction(testDatabase) {
            SchemaUtils.create(Accounts)
            val firstId = UUID.randomUUID()
            val secondId = UUID.randomUUID()

            val testData = listOf(
                Account("account_inserted", 10, firstId),
                Account("account_inserted", 5, secondId)
            )

            Accounts.singleSQLBatchInsert(testData) {
                this[Accounts.name] = it.name
                this[Accounts.balance] = it.balance
                this[Accounts.reference] = it.reference
            }

            val upsertData = listOf(
                Account("account_upserted", 10, firstId),
                Account("account_upserted", 5, secondId),
                Account("account_upserted", 15, UUID.randomUUID())
            )

            Accounts.singleSqlBatchUpsert(upsertData, Accounts.reference, where = { Accounts.balance greater 5 }) { statement ->
                this[Accounts.name] = statement.name
                this[Accounts.balance] = statement.balance
                this[Accounts.reference] = statement.reference
            }

            assertEquals(3, Accounts.selectAll().count())
            assertEquals(2, Accounts.select { Accounts.name eq "account_upserted" }.count())
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
