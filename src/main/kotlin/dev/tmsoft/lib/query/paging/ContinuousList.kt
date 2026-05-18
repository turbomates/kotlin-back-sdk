@file:Suppress("NestedBlockDepth", "SpreadOperator")

package dev.tmsoft.lib.query.paging

import dev.tmsoft.lib.serialization.elementSerializer
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.exposed.v1.core.AndOp
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnSet
import org.jetbrains.exposed.v1.core.ops.AllAnyFromBaseOp
import org.jetbrains.exposed.v1.core.ComparisonOp
import org.jetbrains.exposed.v1.core.CompoundBooleanOp
import org.jetbrains.exposed.v1.core.Concat
import org.jetbrains.exposed.v1.core.Count
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.CustomOperator
import org.jetbrains.exposed.v1.core.EqOp
import org.jetbrains.exposed.v1.core.Exists
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.IExpressionAlias
import org.jetbrains.exposed.v1.core.IsDistinctFromOp
import org.jetbrains.exposed.v1.core.IsNotDistinctFromOp
import org.jetbrains.exposed.v1.core.IsNotNullOp
import org.jetbrains.exposed.v1.core.IsNullOp
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.LiteralOp
import org.jetbrains.exposed.v1.core.LowerCase
import org.jetbrains.exposed.v1.core.Max
import org.jetbrains.exposed.v1.core.Min
import org.jetbrains.exposed.v1.core.ModOp
import org.jetbrains.exposed.v1.core.NoOpConversion
import org.jetbrains.exposed.v1.core.NotExists
import org.jetbrains.exposed.v1.core.NotOp
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryParameter
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SubQueryOp
import org.jetbrains.exposed.v1.core.Sum
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.UpperCase
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.compoundAnd
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.ops.InListOrNotInListBaseOp
import org.jetbrains.exposed.v1.core.ops.SingleValueInListOp
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select

suspend fun <T> Query.toContinuousList(
    page: PagingParameters,
    effector: suspend ResultRow.() -> T,
    sortingParameters: List<SortingParameter> = emptyList(),
    includeCount: Boolean = false
): ContinuousList<T> {
    return toContinuousListBuilder(page, sortingParameters, includeCount) { this.map { effector(it) } }
}

@JvmName("toContinuousListIterable")
suspend fun <T> Query.toContinuousList(
    page: PagingParameters,
    effector: suspend Iterable<ResultRow>.() -> List<T>,
    sortingParameters: List<SortingParameter> = emptyList(),
    includeCount: Boolean = false
): ContinuousList<T> {
    return toContinuousListBuilder(page, sortingParameters, includeCount) { effector() }
}

@Suppress("SpreadOperator")
suspend fun <T> Query.toContinuousListBuilder(
    page: PagingParameters,
    sortingParameters: List<SortingParameter> = emptyList(),
    includeCount: Boolean = false,
    effector: suspend Query.() -> List<T>
): ContinuousList<T> = coroutineScope {
    var count: Long? = null
    if (targets.count() > 1) {
        val rootTable = rootTable() ?: targets.first()
        val primaryKey = rootTable.primaryKey?.columns?.first()
        if (primaryKey != null) {
            val countQuery = Query(set, where)
            adjustWhereIn(primaryKey, sortingParameters, page.pageSize + 1, page.offset)
            if (includeCount) {
                count = countQuery.uniqueCount(primaryKey)
            }
        }
    } else {
        sortedWith(sortingParameters)
        count = copy().count()
        limit(page.pageSize + 1)
        offset(page.offset)
    }

    var result = effector()
    var hasMore = false
    if (result.size > page.pageSize) {
        hasMore = true
        result = result.dropLast(1)
    }
    ContinuousList(result, page.pageSize, page.currentPage, hasMore, count)
}

fun <T> Query.adjustWhereIn(
    primaryKey: Column<T>,
    sortingParameters: List<SortingParameter>,
    limit: Int,
    offset: Long
) {
    val (idsQuery, idExpression) = idSubQuery(primaryKey, sortingParameters)
    val ids = idsQuery
        .limit(limit)
        .offset(offset)
        .map { it[idExpression] }
    adjustWhere { SingleValueInListOp(primaryKey, ids) }
    sortedWith(sortingParameters)
}

private fun Query.sortedWith(sortingParameters: List<SortingParameter>): Query {
    return apply {
        buildSortingParameters(sortingParameters).run { if (isNotEmpty()) orderBy(*this) }
    }
}

fun Query.buildSortingParameters(sortingParameters: List<SortingParameter>): Array<Pair<Expression<*>, SortOrder>> {
    val columns = targets.flatMap { it.columns }
    return sortingParameters
        .associate { sortingParameter ->
            val column = columns.find { sortingParameter.name == it.name }
                ?: throw IllegalArgumentException("Unknown sorting parameter: ${sortingParameter.name}")
            column to sortingParameter.sortOrder
        }
        .toList()
        .toTypedArray()
}

private fun <T> Query.distinctCount(primaryKey: Column<T>): Long {
    return Query(set, where)
        .adjustSelect { select(primaryKey) }
        .withDistinct()
        .count()
}

private fun <T> Query.uniqueCount(primaryKey: Column<T>): Long {
    return rootQuery(primaryKey, emptyArray())?.count() ?: distinctCount(primaryKey)
}

private fun <T> Query.idSubQuery(
    primaryKey: Column<T>,
    sortingParameters: List<SortingParameter>
): Pair<Query, Expression<T>> {
    return rootIdSubQuery(primaryKey, sortingParameters)?.let { it to primaryKey }
        ?: joinToExistsIdQuery(primaryKey, sortingParameters)?.let { it to primaryKey }
        ?: distinctSubQuery(primaryKey, sortingParameters)
}

private fun <T> Query.rootIdSubQuery(
    primaryKey: Column<T>,
    sortingParameters: List<SortingParameter>
): Query? {
    val sorting = buildSortingParametersForRoot(sortingParameters) ?: return null
    val existingSorting = orderByExpressions.toTypedArray()
    return rootQuery(primaryKey, sorting + existingSorting)?.apply {
        sorting.forEach { (expression, sortOrder) -> orderBy(expression, sortOrder) }
        existingSorting.forEach { (expression, sortOrder) -> orderBy(expression, sortOrder) }
    }
}

private fun <T> Query.rootQuery(
    primaryKey: Column<T>,
    sorting: Array<Pair<Expression<*>, SortOrder>>
): Query? {
    val rootTable = rootTable() ?: return null
    val rootColumns = rootTable.columns.toSet()
    if (primaryKey !in rootColumns) return null
    if (sorting.any { (expression, _) -> !expression.referencesOnly(rootTable) }) return null
    if (where?.referencesOnly(rootTable) == false) return null

    return Query(rootTable.select(primaryKey).set, where)
}

private fun Query.rootTable(): Table? {
    return when (val source = set.source) {
        is Join -> source.table as? Table
        is Table -> source
        else -> null
    }
}

private fun Query.buildSortingParametersForRoot(
    sortingParameters: List<SortingParameter>
): Array<Pair<Expression<*>, SortOrder>>? {
    val columns = rootTable()?.columns ?: return null
    return sortingParameters
        .associate { sortingParameter ->
            val column = columns.find { sortingParameter.name == it.name } ?: return null
            column to sortingParameter.sortOrder
        }
        .toList()
        .toTypedArray()
}

private fun <T> Query.joinToExistsIdQuery(
    primaryKey: Column<T>,
    sortingParameters: List<SortingParameter>
): Query? {
    val join = set.source as? Join ?: return null
    val rootTable = join.table as? Table ?: return null
    if (primaryKey !in rootTable.columns.toSet()) return null

    val sorting = buildSortingParametersForRoot(sortingParameters) ?: return null
    val existingSorting = orderByExpressions.toTypedArray()
    if ((sorting + existingSorting).any { (expr, _) -> !expr.referencesOnly(rootTable) }) return null

    val joinParts = join.joinPartsReflect() ?: return null

    val whereLeaves = where?.flattenAnd() ?: emptyList()
    val joinedTables = joinParts.mapNotNull { it.joinedTable() as? Table }

    val rootConditions = mutableListOf<Op<Boolean>>()
    val tableConditions = joinedTables.associateWith { mutableListOf<Op<Boolean>>() }

    for (condition in whereLeaves) {
        val matched = joinedTables.filter { condition.referencesTable(it) }
        when (matched.size) {
            0 -> {
                if (!condition.referencesTable(rootTable)) return null
                rootConditions.add(condition)
            }
            1 -> tableConditions[matched.first()]?.add(condition)
            else -> return null
        }
    }

    val existsOps = joinParts.mapNotNull { part ->
        val joinedTable = part.joinedTable() as? Table ?: return null
        val onConditions = part.joinConditions() ?: return null
        val chainReferenced = onConditions.any { (l, r) ->
            !(l.referencesOnly(rootTable) || l.referencesOnly(joinedTable)) ||
                !(r.referencesOnly(rootTable) || r.referencesOnly(joinedTable))
        }
        if (chainReferenced) return null
        val onOps = onConditions.map { (l, r) -> EqOp(l, r) }
        val extra = part.additionalConstraint()?.invoke()
        val bodyOps = onOps + tableConditions.getValue(joinedTable) + listOfNotNull(extra)
        Exists(joinedTable.select(intLiteral(1)).where(bodyOps.compoundAnd()))
    }

    val finalWhere = (rootConditions + existsOps).takeIf { it.isNotEmpty() }?.compoundAnd()

    return Query(rootTable.select(primaryKey).set, finalWhere).apply {
        (sorting + existingSorting).forEach { (expr, order) -> orderBy(expr, order) }
    }
}

@Suppress("UNCHECKED_CAST")
private fun Join.joinPartsReflect(): List<Any>? = runCatching {
    Join::class.java.getDeclaredField("joinParts")
        .apply { isAccessible = true }
        .get(this) as List<Any>
}.getOrNull()

private fun Any.joinedTable(): ColumnSet? = runCatching {
    javaClass.getDeclaredField("joinPart")
        .apply { isAccessible = true }
        .get(this) as ColumnSet
}.getOrNull()

@Suppress("UNCHECKED_CAST")
private fun Any.joinConditions(): List<Pair<Expression<*>, Expression<*>>>? = runCatching {
    javaClass.getDeclaredField("conditions")
        .apply { isAccessible = true }
        .get(this) as List<Pair<Expression<*>, Expression<*>>>
}.getOrNull()

@Suppress("UNCHECKED_CAST")
private fun Any.additionalConstraint(): (() -> Op<Boolean>)? = runCatching {
    javaClass.getDeclaredField("additionalConstraint")
        .apply { isAccessible = true }
        .get(this) as? (() -> Op<Boolean>)
}.getOrNull()

private fun Expression<Boolean>.flattenAnd(): List<Op<Boolean>> = when (this) {
    is AndOp -> expressions().orEmpty().flatMap { it.flattenAnd() }
    else -> listOfNotNull(this as? Op<Boolean>)
}

private fun Expression<*>.referencesTable(table: Table): Boolean = when (this) {
    is Column<*> -> this.table == table
    is CompoundBooleanOp -> expressions()?.any { it.referencesTable(table) } == true
    is NotOp<*> -> expr.referencesTable(table)
    is ComparisonOp -> expr1.referencesTable(table) || expr2.referencesTable(table)
    is IsNullOp -> expr.referencesTable(table)
    is IsNotNullOp -> expr.referencesTable(table)
    is IsDistinctFromOp -> expression1.referencesTable(table) || expression2.referencesTable(table)
    is IsNotDistinctFromOp -> expression1.referencesTable(table) || expression2.referencesTable(table)
    is InListOrNotInListBaseOp<*> -> (expr as? Expression<*>)?.referencesTable(table) == true
    is LowerCase<*> -> expr.referencesTable(table)
    is UpperCase<*> -> expr.referencesTable(table)
    is Concat -> expr.any { it.referencesTable(table) }
    is CustomFunction<*> -> expr.any { it.referencesTable(table) }
    is CustomOperator<*> -> expr1.referencesTable(table) || expr2.referencesTable(table)
    is ModOp<*, *, *> -> expr1.referencesTable(table) || expr2.referencesTable(table)
    is NoOpConversion<*, *> -> expr.referencesTable(table)
    is AllAnyFromBaseOp<*, *> -> when (val s = subSearch) {
        is Expression<*> -> s.referencesTable(table)
        is Table -> s == table
        else -> false
    }
    else -> false
}

private fun <T> Query.distinctSubQuery(
    primaryKey: Column<T>,
    sortingParameters: List<SortingParameter>
): Pair<Query, Expression<T>> {
    val sorting = buildSortingParameters(sortingParameters) + orderByExpressions
    val sortingAliases = sorting.mapIndexed { index, (expression, sortOrder) ->
        expression.alias("sort_field_$index") to sortOrder
    }

    val primaryKeyAlias = primaryKey.alias("uniq_field_id")
    val selectedExpressions = listOf<Expression<*>>(primaryKeyAlias) + sortingAliases.map { it.first }

    val deduplicatedSubQuery = Query(set, where)
        .adjustSelect { select(selectedExpressions) }
        .withDistinctOn(primaryKey to SortOrder.ASC)
        .apply {
            sorting.forEach { (expression, sortOrder) -> orderBy(expression, sortOrder) }
        }
        .alias("subquery")

    val outerSort = sortingAliases
        .map { (expressionAlias, sortOrder) -> deduplicatedSubQuery[expressionAlias] to sortOrder }
        .toTypedArray()

    val selectedPrimaryKey: Expression<T> = deduplicatedSubQuery[primaryKeyAlias]

    val distinctIdsQuery = deduplicatedSubQuery
        .select(selectedPrimaryKey)
        .apply { if (outerSort.isNotEmpty()) orderBy(*outerSort) }

    return distinctIdsQuery to selectedPrimaryKey
}

private fun Expression<*>.referencesOnly(table: Table): Boolean {
    return when (this) {
        Op.TRUE, Op.FALSE -> true
        is Column<*> -> this.table == table
        is QueryParameter<*> -> true
        is LiteralOp<*> -> true
        is IExpressionAlias<*> -> delegate.referencesOnly(table)
        is CompoundBooleanOp -> expressions()?.all { it.referencesOnly(table) } == true
        is NotOp<*> -> expr.referencesOnly(table)
        is ComparisonOp -> expr1.referencesOnly(table) && expr2.referencesOnly(table)
        is IsNullOp -> expr.referencesOnly(table)
        is IsNotNullOp -> expr.referencesOnly(table)
        is IsNotDistinctFromOp -> expression1.referencesOnly(table) && expression2.referencesOnly(table)
        is IsDistinctFromOp -> expression1.referencesOnly(table) && expression2.referencesOnly(table)
        is InListOrNotInListBaseOp<*> -> expr.referencesOnly(table)
        is Exists -> true
        is NotExists -> true
        is SubQueryOp<*> -> expr.referencesOnly(table)
        is LowerCase<*> -> expr.referencesOnly(table)
        is UpperCase<*> -> expr.referencesOnly(table)
        is Concat -> expr.all { it.referencesOnly(table) }
        is Min<*, *> -> expr.referencesOnly(table)
        is Max<*, *> -> expr.referencesOnly(table)
        is Sum<*> -> expr.referencesOnly(table)
        is Count -> expr.referencesOnly(table)
        is CustomFunction<*> -> expr.all { it.referencesOnly(table) }
        is CustomOperator<*> -> expr1.referencesOnly(table) && expr2.referencesOnly(table)
        is ModOp<*, *, *> -> expr1.referencesOnly(table) && expr2.referencesOnly(table)
        is NoOpConversion<*, *> -> expr.referencesOnly(table)
        is AllAnyFromBaseOp<*, *> -> when (val s = subSearch) {
            is Expression<*> -> s.referencesOnly(table)
            is Table -> s == table
            else -> true
        }
        else -> false
    }
}

private fun Any?.referencesOnly(table: Table): Boolean {
    return when (this) {
        null -> true
        is Expression<*> -> referencesOnly(table)
        is Pair<*, *> -> first.referencesOnly(table) && second.referencesOnly(table)
        is Triple<*, *, *> -> first.referencesOnly(table) && second.referencesOnly(table) && third.referencesOnly(table)
        is Iterable<*> -> all { it.referencesOnly(table) }
        else -> true
    }
}

@Suppress("UNCHECKED_CAST")
private fun CompoundBooleanOp.expressions(): List<Expression<Boolean>>? {
    return runCatching {
        CompoundBooleanOp::class.java.getDeclaredField("expressions")
            .apply { isAccessible = true }
            .get(this) as List<Expression<Boolean>>
    }.getOrNull()
}

data class ContinuousList<T>(
    val data: List<T>,
    val pageSize: Int,
    val currentPage: Int,
    val hasMore: Boolean = false,
    val count: Long? = null
)

object ContinuousListSerializer : KSerializer<ContinuousList<*>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ContinuousListDescriptor")

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: ContinuousList<*>) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
        val encoded = output.json.encodeToString(
            ListSerializer(value.data.elementSerializer()) as KSerializer<Any>,
            value.data
        )
        val map = mutableMapOf(
            "pageSize" to JsonPrimitive(value.pageSize),
            "page" to JsonPrimitive(value.currentPage),
            "hasMore" to JsonPrimitive(value.hasMore),
            "data" to output.json.parseToJsonElement(encoded)
        )

        if (value.count != null) map["count"] = JsonPrimitive(value.count)

        val tree = JsonObject(map)
        output.encodeJsonElement(tree)
    }

    override fun deserialize(decoder: Decoder): ContinuousList<*> {
        throw NotImplementedError()
    }
}
