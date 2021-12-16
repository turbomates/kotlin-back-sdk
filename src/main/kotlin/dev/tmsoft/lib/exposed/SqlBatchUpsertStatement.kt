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
        append(transaction.onUpdateSql(batchValues.first().keys, *keys))
    }
}

private fun Transaction.onUpdateSql(values: Iterable<Column<*>>, vararg keys: Column<*>) = buildString {
    if (db.vendor != "postgresql") throw UnsupportedOperationException("SqlBatchUpsertStatement is not implemented for ${db.vendor}")
    append(" ON CONFLICT (${keys.joinToString(",") { identity(it) }})")
    values.filter { it !in keys }.takeIf { it.isNotEmpty() }?.let { fields ->
        append(" DO UPDATE SET ")
        fields.joinTo(this, ", ") { "${identity(it)} = EXCLUDED.${identity(it)}" }
    } ?: append(" DO NOTHING")
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
        println(statement.prepareSQL(TransactionManager.current()))
        statement.execute(TransactionManager.current())
    }

    return emptyList()
}
