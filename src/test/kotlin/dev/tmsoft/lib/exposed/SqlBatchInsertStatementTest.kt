package dev.tmsoft.lib.exposed

import dev.tmsoft.lib.Config
import java.util.UUID
import kotlin.test.assertEquals
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test

class SqlBatchInsertStatementTest {
    @Test
    fun `batch insert`() {
        val database = Database.connect(Config.h2DatabaseUrl, driver = Config.h2Driver, user = Config.h2User, password = Config.h2Password)
        transaction(database) {
            SchemaUtils.create(Accounts)
            (1..5).forEach {
                val testData = (0..300).map {
                    Account("test $it", it, UUID.randomUUID())
                }
                Accounts.singleSQLBatchInsert(testData) {
                    this[Accounts.name] = it.name
                    this[Accounts.balance] = it.balance
                    this[Accounts.reference] = it.reference
                }
            }
            assertEquals(1505, Accounts.selectAll().count())
        }
    }

    data class Account(val name: String, val balance: Int, val reference: UUID)
    object Accounts : IntIdTable("test") {
        val name = varchar("account", 255).nullable()
        val balance = integer("balance")
        val reference = uuid("reference").uniqueIndex("uniq_referenec")
    }
}
