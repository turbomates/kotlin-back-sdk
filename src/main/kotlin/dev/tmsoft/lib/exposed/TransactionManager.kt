@file:Suppress("ForbiddenImport", "INVISIBLE_MEMBER")

package dev.tmsoft.lib.exposed

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Key
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.experimental.withSuspendTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class TransactionManager(
    private val primaryDatabase: Database,
    private val replicaDatabase: List<Database> = emptyList()
) {
    constructor(primaryDatabase: Database) : this(primaryDatabase, listOf(primaryDatabase))

    private suspend fun <T> suspendedTransaction(
        database: Database = primaryDatabase,
        statement: suspend Transaction.() -> T
    ): T {
        return withContext(Dispatchers.IO) {
            val currentContext = coroutineContext[TransactionScope]
            if (currentContext != null) {
                currentContext.tx.withSuspendTransaction {
                    statement()
                }
            } else {
                newSuspendedTransaction(db = database) {
                    this.getOrCreate(CoroutineTransactionContext.key) { coroutineContext }
                    withContext(TransactionScope(this, coroutineContext)) {
                        statement()
                    }
                }
            }
        }
    }

    suspend operator fun <T> invoke(statement: suspend Transaction.() -> T): T =
        suspendedTransaction(
            primaryDatabase,
            statement
        )

    suspend fun <T> readOnlyTransaction(statement: suspend Transaction.() -> T) =
        suspendedTransaction(
            replicaDatabase.random(),
            statement
        )

    // NOTE: doesn't propagate process CoroutineTransactionContext context, pass it explicitly
    fun <T> sync(statement: Transaction.() -> T): T {
        return transaction(primaryDatabase, statement = statement)
    }

    suspend fun <T> async(statement: Transaction.() -> T): Job {
        return withContext(Dispatchers.IO) {
            val currentContext = coroutineContext[TransactionScope]
            if (currentContext != null) {
                suspendedTransactionAsync(currentContext, db = primaryDatabase) {
                    this.getOrCreate(CoroutineTransactionContext.key) { coroutineContext }
                    withContext(TransactionScope(this, coroutineContext)) {
                        statement()
                    }
                }
            } else {
                suspendedTransactionAsync(db = primaryDatabase) {
                    this.getOrCreate(CoroutineTransactionContext.key) { coroutineContext }
                    withContext(TransactionScope(this, coroutineContext)) {
                        statement()
                    }
                }
            }
        }
    }

    // NOTE: doesn't propagate process CoroutineTransactionContext context, pass it explicitly
    fun <T> readOnlySync(statement: Transaction.() -> T): T {
        return transaction(replicaDatabase.random(), statement = statement)
    }
}

suspend fun <T> Transaction.withDatabaseLock(
    id: Int,
    body: suspend () -> T
): T {
    exec("SELECT pg_advisory_xact_lock($id)")
    return body()
}

suspend fun <T> Transaction.withTryDatabaseLock(
    id: Int,
    body: suspend () -> T
): T {
    val locked =
        exec("SELECT pg_try_advisory_xact_lock($id)") { rs ->
            rs.next() && rs.getBoolean(1)
        }
    if (locked != null && !locked) {
        throw LockUnavailable()
    }
    return body()
}

// FIXME: it is crutch, I guess. But alternative is to use suspend methods in all models and pass process data with event
object CoroutineTransactionContext {
    val key = Key<CoroutineContext>()
}

class LockUnavailable : Exception()

class TransactionScope(
    internal val tx: Transaction,
    parent: CoroutineContext
) : CoroutineScope, CoroutineContext.Element {
    private val baseScope = CoroutineScope(parent)
    override val coroutineContext get() = baseScope.coroutineContext + this
    override val key = Companion

    companion object : CoroutineContext.Key<TransactionScope>
}
