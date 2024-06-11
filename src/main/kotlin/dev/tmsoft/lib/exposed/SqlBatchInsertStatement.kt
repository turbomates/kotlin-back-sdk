package dev.tmsoft.lib.exposed

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.jetbrains.exposed.sql.transactions.TransactionManager

open class SqlBatchInsertStatement(
    private val table: Table,
    private val ignore: Boolean
) : UpdateBuilder<List<Int>>(StatementType.INSERT, listOf(table)) {
    private val batchValues: MutableList<Map<Column<*>, Any?>> = mutableListOf()
    override val isAlwaysBatch = true
    private val prepareSQLArguments: MutableList<Pair<Column<*>, Any?>> = mutableListOf()
    private val allColumnsInDataSet = mutableSetOf<Column<*>>()
    private val arguments: MutableList<Iterable<Pair<IColumnType<*>, Any?>>> = mutableListOf()
    protected val columns
        get() = batchValues.first().keys

    internal fun addBatch() {
        if (batchValues.isEmpty()) {
            allColumnsInDataSet.addAll(values.keys)
        }
        val nullableColumns by lazy { allColumnsInDataSet.filter { it.columnType.nullable } }
        val valuesAndDefaults = valuesAndDefaults(values)
        val args = (valuesAndDefaults + (nullableColumns - valuesAndDefaults.keys).associateWith { null }).toList()
            .sortedBy { it.first }
        if (batchValues.isEmpty()) {
            prepareSQLArguments.addAll(args)
        }
        val builder = QueryBuilder(true)
        args.filter { (_, value) ->
            value != "DEFAULT"
        }.forEach { (column, value) ->
            builder.registerArgument(column, value)
        }
        arguments.add(builder.args)
        batchValues.add(LinkedHashMap(values))
        values.clear()
    }

    override fun arguments(): Iterable<Iterable<Pair<IColumnType<*>, Any?>>> {
        return arguments
    }

    override fun prepareSQL(transaction: Transaction, prepared: Boolean): String {
        val builder = QueryBuilder(true)
        val sql = if (prepareSQLArguments.isEmpty()) ""
        else with(builder) {
            prepareSQLArguments.appendTo(prefix = "VALUES (", postfix = ")") { (col, value) ->
                registerArgument(col, value)
            }
            toString()
        }
        return transaction.db.dialect.functionProvider.insert(
            ignore,
            table,
            prepareSQLArguments.map { it.first },
            sql,
            transaction
        )
    }

    override fun PreparedStatementApi.executeInternal(transaction: Transaction): List<Int> {
        return executeBatch()
    }

    private fun valuesAndDefaults(values: Map<Column<*>, Any?> = this.values): Map<Column<*>, Any?> {
        val columnsWithNotNullDefault = targets.flatMap { it.columns }.filter {
            it.defaultValueFun != null && it !in values.keys
        }
        return values + columnsWithNotNullDefault.map { it to (it.defaultValueFun?.invoke() ?: "DEFAULT") }
    }
}

fun <T : Table, E> T.singleSQLBatchInsert(
    data: Iterable<E>,
    ignore: Boolean = false,
    body: SqlBatchInsertStatement.(E) -> Unit
): List<ResultRow> {
    if (data.count() == 0) return emptyList()

    val statement = SqlBatchInsertStatement(this, ignore)
    data.forEach {
        statement.body(it)
        statement.addBatch()
    }
    if (statement.arguments().count() > 0) {
        statement.execute(TransactionManager.current())
    }
    return emptyList()
}
