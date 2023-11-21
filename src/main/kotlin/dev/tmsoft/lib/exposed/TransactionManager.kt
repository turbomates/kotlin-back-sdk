@file:Suppress("ForbiddenImport", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package dev.tmsoft.lib.exposed

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Key
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.TransactionScope
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction

class TransactionManager(private val database: Database, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    suspend operator fun <T> invoke(statement: suspend Transaction.() -> T): T {
        return suspendedTransaction(database, statement)
    }

    suspend fun <T> async(statement: suspend Transaction.() -> T): Deferred<T> {
        return suspendedTransactionAsync(coroutineContext + dispatcher, db = database, statement = statement)
    }

    fun <T> sync(statement: Transaction.() -> T): T {
        return transaction(database, statement = statement)
    }

    private suspend fun <T> suspendedTransaction(
        database: Database = this.database,
        statement: suspend Transaction.() -> T
    ): T {
        return withContext(dispatcher) {
            val current = coroutineContext[TransactionScope]
            if (current?.tx == null) {
                newSuspendedTransaction(db = database) {
                    this.getOrCreate(CoroutineTransactionContext.key) { coroutineContext }
                    statement()
                }
            } else {
                val currentContext = current.tx.value.getUserData(CoroutineTransactionContext.key)
                if (currentContext == null) {
                    // Context can be absent either if transaction was rolled back or it wasn't created by TransactionManager
                    // In both cases we don't want to propagate context
                    current.tx.value.suspendedTransaction {
                        statement()
                    }
                } else {
                    current.tx.value.suspendedTransaction {
                        this.putUserData(CoroutineTransactionContext.key, currentContext)
                        statement()
                    }
                }
            }
        }
    }
}

object CoroutineTransactionContext {
    val key = Key<CoroutineContext>()
}

suspend fun <T> Transaction.withDataBaseLock(id: Int, body: suspend () -> T): T {
    exec("SELECT pg_advisory_xact_lock($id)")
    return body()
}
