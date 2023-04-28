package dev.tmsoft.lib.exposed

import dev.tmsoft.lib.buildConfiguration
import java.sql.Connection
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.api.ExposedSavepoint
import org.jetbrains.exposed.sql.transactions.TransactionInterface
import org.jetbrains.exposed.sql.transactions.TransactionManager

class ExposedTestTransactionManager(
    private val db: Database,
    @Volatile override var defaultIsolationLevel: Int = db.config.defaultIsolationLevel,
    @Volatile override var defaultRepetitionAttempts: Int = db.config.defaultRepetitionAttempts,
    override var defaultReadOnly: Boolean,
) : TransactionManager {
    var transaction: Transaction? = null

    override fun bindTransactionToThread(transaction: Transaction?) {
        "Should be an empty"
    }

    override fun currentOrNull(): Transaction? {
        return transaction
    }

    override fun newTransaction(isolation: Int, readOnly: Boolean, outerTransaction: Transaction?): Transaction {
        transaction = Transaction(
            TestTransaction(
                db = db,
                transactionIsolation = defaultIsolationLevel,
                manager = this,
                outerTransaction = outerTransaction ?: transaction,
                readOnly
            )
        )
        return transaction!!
    }

    private class TestTransaction(
        override val db: Database,
        override val transactionIsolation: Int,
        val manager: ExposedTestTransactionManager,
        override val outerTransaction: Transaction?,
        override val readOnly: Boolean
    ) : TransactionInterface {
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
        override val connection = outerTransaction?.connection ?: db.connector().apply {
            autoCommit = false
            transactionIsolation = this@TestTransaction.transactionIsolation
        }

        private val shouldUseSavePoints = outerTransaction != null && db.useNestedTransactions
        private var savepoint: ExposedSavepoint? = if (shouldUseSavePoints) {
            connection.setSavepoint(savepointName)
        } else null

        override fun commit() {
            if (!shouldUseSavePoints) {
                connection.commit()
            }
        }

        override fun rollback() {
            if (!connection.isClosed) {
                if (shouldUseSavePoints) {
                    connection.rollback(savepoint!!)
                    savepoint = connection.setSavepoint(savepointName)
                } else {
                    connection.rollback()
                }
            }
        }

        override fun close() {
            try {
                if (!shouldUseSavePoints) {
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
    }
}

internal val testDatabase by lazy {
    val config = buildConfiguration()
    Database.connect(
        config.url,
        user = config.user,
        password = config.password.value,
        driver = "org.postgresql.Driver",
        databaseConfig = DatabaseConfig { useNestedTransactions = true },
        manager = { database ->
            ExposedTestTransactionManager(
                database,
                Connection.TRANSACTION_READ_COMMITTED,
                defaultReadOnly = false
            )
        }
    )
}

