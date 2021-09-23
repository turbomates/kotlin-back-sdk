package dev.tmsoft.lib.exposed

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import java.sql.Connection
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.api.ExposedSavepoint
import org.jetbrains.exposed.sql.transactions.DEFAULT_REPETITION_ATTEMPTS
import org.jetbrains.exposed.sql.transactions.TransactionInterface
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedTestTransactionManager(
    private val db: Database,
    @Volatile override var defaultIsolationLevel: Int,
    @Volatile override var defaultRepetitionAttempts: Int
) : TransactionManager {
    var transaction: Transaction? = null
    override fun bindTransactionToThread(transaction: Transaction?) {
        "Should be an empty"
    }

    override fun currentOrNull(): Transaction? {
        return transaction
    }

    override fun newTransaction(isolation: Int, outerTransaction: Transaction?): Transaction {
        transaction = Transaction(
            TestTransaction(
                db = db,
                transactionIsolation = defaultIsolationLevel,
                manager = this,
                outerTransaction = outerTransaction ?: transaction
            )
        )
        return transaction!!
    }

    private class TestTransaction(
        override val db: Database,
        override val transactionIsolation: Int,
        val manager: ExposedTestTransactionManager,
        override val outerTransaction: Transaction?
    ) : TransactionInterface {

        override val connection = outerTransaction?.connection ?: db.connector().apply {
            autoCommit = false
            transactionIsolation = this@TestTransaction.transactionIsolation
        }

        private val useSavePoints = outerTransaction != null && db.useNestedTransactions
        private var savepoint: ExposedSavepoint? = if (useSavePoints) {
            connection.setSavepoint(savepointName)
        } else null

        override fun commit() {
            if (!useSavePoints) {
                connection.commit()
            }
        }

        override fun rollback() {
            if (!connection.isClosed) {
                if (useSavePoints && savepoint != null) {
                    connection.rollback(savepoint!!)
                    savepoint = connection.setSavepoint(savepointName)
                } else {
                    connection.rollback()
                }
            }
        }

        override fun close() {
            try {
                if (!useSavePoints) {
                    connection.close()
                } else {
                    savepoint?.let {
                        connection.releaseSavepoint(it)
                        savepoint = null
                    }
                }
            } finally {
                manager.transaction = outerTransaction
            }
        }

        private val savepointName: String
            get() {
                var nestedLevel = 0
                var currentTransaction = outerTransaction
                while (currentTransaction != null) {
                    nestedLevel++
                    currentTransaction = currentTransaction.outerTransaction
                }
                return "Exposed_savepoint_$nestedLevel"
            }
    }
}

fun <T> rollbackTransaction(db: Database = testDatabase, statement: Transaction.() -> T): T {
    val postgres = EmbeddedPostgres.builder()
        .setPort(12346).start()
    val result = transaction(db) { val result = statement(); rollback(); result }
    postgres.close()
    return result
}

internal val testDatabase by lazy {
    Database.connect(
        "jdbc:postgresql://localhost:12346/postgres?user=postgres&password=postgres",
        user = "postgres",
        password = "",
        driver = "org.postgresql.Driver",
        manager = { database ->
            database.useNestedTransactions = true
            ExposedTestTransactionManager(
                database,
                Connection.TRANSACTION_READ_COMMITTED,
                DEFAULT_REPETITION_ATTEMPTS
            )
        }
    )
}
