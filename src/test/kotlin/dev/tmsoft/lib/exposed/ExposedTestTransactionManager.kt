package dev.tmsoft.lib.exposed

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.Masked
import com.sksamuel.hoplite.PropertySource
import dev.tmsoft.lib.config.hoplite.EnvironmentVariablesPropertySource
import java.sql.Connection
import org.jetbrains.exposed.sql.DEFAULT_REPETITION_ATTEMPTS
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.api.ExposedSavepoint
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

internal val testDatabase by lazy {
    val config = buildConfiguration()
    Database.connect(
        config.url,
        user = config.user,
        password = config.password.toString(),
        driver = "org.postgresql.Driver",
        databaseConfig = DatabaseConfig { useNestedTransactions = true },
        manager = { database ->
            ExposedTestTransactionManager(
                database,
                Connection.TRANSACTION_READ_COMMITTED,
                DEFAULT_REPETITION_ATTEMPTS
            )
        }
    )
}
data class Config(val jdbc: Jdbc)
data class Jdbc(val url: String, val user: String, val password: Masked)
fun buildConfiguration(): Jdbc {
    return ConfigLoader.Builder()
        .addSource(PropertySource.resource("/local.properties", optional = true))
        .addSource(PropertySource.resource("/default.properties", optional = true))
        .build()
        .loadConfigOrThrow<Config>().jdbc
}
