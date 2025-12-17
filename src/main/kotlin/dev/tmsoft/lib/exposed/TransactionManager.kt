@file:Suppress("ForbiddenImport", "INVISIBLE_MEMBER")

package dev.tmsoft.lib.exposed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

open class TransactionManager(
    private val primaryDatabase: Database,
    private val replicaDatabase: List<Database> = emptyList()
) {
    constructor(primaryDatabase: Database) : this(primaryDatabase, listOf(primaryDatabase))

    suspend operator fun <T> invoke(statement: suspend JdbcTransaction.() -> T): T =
        withContext(Dispatchers.IO) {
            suspendTransaction(primaryDatabase, statement = statement)
        }


    suspend fun <T> readOnlyTransaction(statement: suspend JdbcTransaction.() -> T) =
        withContext(Dispatchers.IO) {
            suspendTransaction(
                replicaDatabase.random(),
                statement = statement
            )
        }

    fun <T> sync(statement: Transaction.() -> T): T {
        return transaction(primaryDatabase, statement = statement)
    }


    fun <T> readOnlySync(statement: Transaction.() -> T): T {
        return transaction(replicaDatabase.random(), statement = statement)
    }
}

suspend fun <T> JdbcTransaction.withDatabaseLock(
    id: Int,
    body: suspend () -> T
): T {
    exec("SELECT pg_advisory_xact_lock($id)")
    return body()
}

suspend fun <T> JdbcTransaction.withTryDatabaseLock(
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

class LockUnavailable : Exception()
