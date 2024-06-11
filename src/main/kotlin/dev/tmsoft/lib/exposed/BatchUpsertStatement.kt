package dev.tmsoft.lib.exposed

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction

open class BatchUpsertStatement(
    table: Table,
    vararg val keys: Column<*>,
    val onUpdate: List<Pair<Column<*>, Expression<*>>>?,
    val onUpdateExclude: List<Column<*>>?,
    val where: Op<Boolean>? = null,
    shouldReturnGeneratedValues: Boolean = true
) : BaseBatchInsertStatement(table, ignore = false, shouldReturnGeneratedValues) {

    override fun prepareSQL(transaction: Transaction, prepared: Boolean): String {
        val functionProvider = when (val dialect = transaction.db.dialect) {
            else -> dialect.functionProvider
        }
        return functionProvider.upsert(
            table,
            arguments!!.first(),
            onUpdate,
            onUpdateExclude,
            where,
            transaction,
            keys = keys
        )
    }

    override fun isColumnValuePreferredFromResultSet(column: Column<*>, value: Any?): Boolean {
        return isEntityIdClientSideGeneratedUUID(column) ||
                super.isColumnValuePreferredFromResultSet(column, value)
    }
}
