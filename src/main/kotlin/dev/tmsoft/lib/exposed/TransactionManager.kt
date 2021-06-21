package dev.tmsoft.lib.exposed

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction

class TransactionManager(private val database: Database) {
    suspend operator fun <T> invoke(statement: suspend Transaction.() -> T): T {
        return newSuspendedTransaction(Dispatchers.IO, db = database, statement = statement)
    }

    suspend fun <T> async(statement: suspend Transaction.() -> T): Deferred<T> {
        return suspendedTransactionAsync(Dispatchers.IO, db = database, statement = statement)
    }

    fun <T> sync(statement: Transaction.() -> T): T {
        return transaction(database, statement = statement)
    }
}

suspend fun <T> Transaction.withDataBaseLock(id: Int, body: suspend () -> T) {
    try {
        exec("SELECT pg_advisory_lock($id)")
        body()
    } finally {
        exec("SELECT pg_advisory_unlock($id)")
    }
}
