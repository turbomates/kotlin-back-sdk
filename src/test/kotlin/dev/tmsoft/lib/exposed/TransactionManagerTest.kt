package dev.tmsoft.lib.exposed

import dev.tmsoft.lib.Config.h2Driver
import dev.tmsoft.lib.Config.h2Password
import dev.tmsoft.lib.Config.h2User
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

class TransactionManagerTest {
    object RollbackTable : IntIdTable("transaction_manager_rollback_test") {
        val value = varchar("value", 64)
    }

    @Test
    fun `sync transaction rolls back insert when exception is thrown`() {
        val database = Database.connect(
            "jdbc:h2:mem:transaction_manager_rollback;DB_CLOSE_DELAY=-1;MODE=MySQL",
            driver = h2Driver,
            user = h2User,
            password = h2Password
        )
        val transactionManager = TransactionManager(database)

        transactionManager.sync {
            SchemaUtils.create(RollbackTable)
        }

        assertFailsWith<IllegalStateException> {
            transactionManager.sync {
                RollbackTable.insert {
                    it[value] = "should_be_rolled_back"
                }
                throw IllegalStateException("boom")
            }
        }

        val persistedRows = transactionManager.sync {
            RollbackTable.selectAll().toList().size
        }

        assertEquals(0, persistedRows)
    }

    @Test
    fun `sync transaction inside outer transaction rolls back only nested scope`() {
        val database = Database.connect(
            "jdbc:h2:mem:transaction_manager_nested_rollback;DB_CLOSE_DELAY=-1;MODE=MySQL",
            driver = h2Driver,
            user = h2User,
            password = h2Password,
            databaseConfig = DatabaseConfig {
                useNestedTransactions = true
            }
        )
        val transactionManager = TransactionManager(database)

        transactionManager.sync {
            SchemaUtils.create(RollbackTable)
        }

        transaction(database) {
            assertFailsWith<IllegalStateException> {
                transactionManager.sync {
                    RollbackTable.insert {
                        it[value] = "should_be_rolled_back"
                    }
                    throw IllegalStateException("boom")
                }
            }

            RollbackTable.insert {
                it[value] = "committed_from_outer_transaction"
            }
        }

        val persistedRows = transactionManager.sync {
            RollbackTable.selectAll().toList().map { it[RollbackTable.value] }
        }

        assertEquals(listOf("committed_from_outer_transaction"), persistedRows)
    }
}
