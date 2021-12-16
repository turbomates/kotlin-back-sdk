package dev.tmsoft.lib.exposed

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.lang.UnsupportedOperationException

class SqlBatchUpsertStatement(
    table: Table,
    ignore: Boolean = false,
    private vararg val keys: Column<*>
) : SqlBatchInsertStatement(table, ignore) {
    override fun prepareSQL(transaction: Transaction) = buildString {
        append(super.prepareSQL(transaction))
        append(prepareOnConflictSQL(transaction))
    }

    private fun prepareOnConflictSQL(transaction: Transaction): String {
        return when (transaction.db.vendor) {
            "postgresql" -> prepareOnConflictPostgreSQL(transaction)
            "mysql" -> prepareOnConflictMySQL(transaction)
            else -> throw UnsupportedOperationException("SqlBatchUpsertStatement is not implemented for ${transaction.db.vendor}")
        }
    }

    private fun prepareOnConflictPostgreSQL(transaction: Transaction): String = buildString {
        append(" ON CONFLICT (${keys.joinToString(",") { transaction.identity(it) }})")
        columns.filter { it !in keys }.takeIf { it.isNotEmpty() }?.let { fields ->
            append(" DO UPDATE SET ")
            fields.joinTo(this, ", ") { "${transaction.identity(it)} = EXCLUDED.${transaction.identity(it)}" }
        } ?: append(" DO NOTHING")
    }

    private fun prepareOnConflictMySQL(transaction: Transaction): String = buildString {
        append(" ON DUPLICATE KEY UPDATE ")
        columns.joinTo(this, ", ") { "${transaction.identity(it)} = VALUES(${transaction.identity(it)})" }
    }
}

fun <T : Table, E> T.singleSqlBatchUpsert(
    data: Iterable<E>,
    vararg keys: Column<*>,
    body: SqlBatchUpsertStatement.(E) -> Unit
): List<ResultRow> {
    if (data.count() == 0) return emptyList()

    val statement = SqlBatchUpsertStatement(this, keys = keys)
    data.forEach {
        statement.body(it)
        statement.addBatch()
    }
    if (statement.arguments().isNotEmpty()) {
        statement.execute(TransactionManager.current())
    }

    return emptyList()
}
