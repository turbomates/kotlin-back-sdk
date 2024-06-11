package dev.tmsoft.lib.exposed

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.BatchDataInconsistentException
import org.jetbrains.exposed.sql.transactions.TransactionManager

fun <T : Table, E : Any> T.batchUpsert(
    data: Iterable<E>,
    vararg keys: Column<*>,
    onUpdate: List<Pair<Column<*>, Expression<*>>>? = null,
    onUpdateExclude: List<Column<*>>? = null,
    shouldReturnGeneratedValues: Boolean = true,
    where: Op<Boolean>? = null,
    body: BatchUpsertStatement.(E) -> Unit
): List<ResultRow> {
    return batchUpsert(
        data.iterator(),
        onUpdate,
        onUpdateExclude,
        shouldReturnGeneratedValues,
        where = where,
        keys = keys,
        body = body
    )
}

fun <T : Table, E : Any> T.batchUpsert(
    data: Sequence<E>,
    vararg keys: Column<*>,
    onUpdate: List<Pair<Column<*>, Expression<*>>>? = null,
    onUpdateExclude: List<Column<*>>? = null,
    shouldReturnGeneratedValues: Boolean = true,
    where: Op<Boolean>? = null,
    body: BatchUpsertStatement.(E) -> Unit
): List<ResultRow> {
    return batchUpsert(
        data.iterator(),
        onUpdate,
        onUpdateExclude,
        shouldReturnGeneratedValues,
        where = where,
        keys = keys,
        body = body
    )
}

private fun <T : Table, E> T.batchUpsert(
    data: Iterator<E>,
    onUpdate: List<Pair<Column<*>, Expression<*>>>? = null,
    onUpdateExclude: List<Column<*>>? = null,
    shouldReturnGeneratedValues: Boolean = true,
    where: Op<Boolean>?,
    vararg keys: Column<*>,
    body: BatchUpsertStatement.(E) -> Unit
): List<ResultRow> = executeBatch(data, body) {
    BatchUpsertStatement(
        this,
        *keys,
        where = where,
        onUpdate = onUpdate,
        onUpdateExclude = onUpdateExclude,
        shouldReturnGeneratedValues = shouldReturnGeneratedValues
    )
}

private fun <E, S : BaseBatchInsertStatement> executeBatch(
    data: Iterator<E>,
    body: S.(E) -> Unit,
    newBatchStatement: () -> S
): List<ResultRow> {
    if (!data.hasNext()) return emptyList()

    var statement = newBatchStatement()

    val result = ArrayList<ResultRow>()
    fun S.handleBatchException(removeLastData: Boolean = false, body: S.() -> Unit) {
        try {
            body()
            if (removeLastData) validateLastBatch()
        } catch (e: BatchDataInconsistentException) {
            if (this.data.size == 1) {
                throw e
            }
            val notTheFirstBatch = this.data.size > 1
            if (notTheFirstBatch) {
                if (removeLastData) {
                    removeLastBatch()
                }
                execute(TransactionManager.current())
                result += resultedValues.orEmpty()
            }
            statement = newBatchStatement()
            if (removeLastData && notTheFirstBatch) {
                statement.addBatch()
                statement.body()
                statement.validateLastBatch()
            }
        }
    }

    data.forEach { element ->
        statement.handleBatchException { addBatch() }
        statement.handleBatchException(true) { body(element) }
    }
    if (statement.arguments().isNotEmpty()) {
        statement.execute(TransactionManager.current())
        result += statement.resultedValues.orEmpty()
    }
    return result
}
