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
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.LiteralOp
import org.jetbrains.exposed.v1.core.LowerCase
import org.jetbrains.exposed.v1.core.Max
import org.jetbrains.exposed.v1.core.Min
import org.jetbrains.exposed.v1.core.ModOp
import org.jetbrains.exposed.v1.core.NoOpConversion
import org.jetbrains.exposed.v1.core.NotExists
import org.jetbrains.exposed.v1.core.NotOp
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.OrOp
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
    return (rootQuery(primaryKey, emptyArray()) ?: rootExistsQuery(primaryKey))?.count() ?: distinctCount(primaryKey)
}

private fun <T> Query.idSubQuery(
    primaryKey: Column<T>,
    sortingParameters: List<SortingParameter>
): Pair<Query, Expression<T>> {
    return rootIdSubQuery(primaryKey, sortingParameters)?.let { it to primaryKey }
        ?: existsIdSubQuery(primaryKey, sortingParameters)?.let { it to primaryKey }
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

private fun <T> Query.existsIdSubQuery(
    primaryKey: Column<T>,
    sortingParameters: List<SortingParameter>
): Query? {
    val rootTable = rootTable() ?: return null
    val sorting = buildSortingParametersForRoot(sortingParameters) ?: return null
    val allSorting = sorting + orderByExpressions.toTypedArray()
    if (allSorting.any { (expression, _) -> !expression.referencesOnly(rootTable) }) return null
    return rootExistsQuery(primaryKey)?.apply {
        allSorting.forEach { (expression, sortOrder) -> orderBy(expression, sortOrder) }
    }
}

/**
 * The ids of the root table with the conditions on each joined table moved into an EXISTS, so the rows are
 * neither multiplied by 1:N joins nor joined at all. The page and the count both read it.
 *
 * An INNER join keeps filtering through its EXISTS even without conditions. A LEFT join without conditions never
 * filters and is left out; with conditions it becomes an EXISTS only when they are false for the all-NULL row an
 * unmatched root row gets, otherwise a root row without a match would be lost. Other join types, lateral joins
 * and conditions that cannot be attributed to the root and one joined table give null.
 */
private fun <T> Query.rootExistsQuery(primaryKey: Column<T>): Query? {
    val join = set.source as? Join ?: return null
    val rootTable = join.table as? Table ?: return null
    if (primaryKey !in rootTable.columns.toSet()) return null

    val joinParts = join.joinPartsReflect() ?: return null
    val joinedTables = joinParts.map { it.table as? Table ?: return null }

    val rootConditions = mutableListOf<Op<Boolean>>()
    val tableConditions = joinedTables.associateWith { mutableListOf<Op<Boolean>>() }
    for (condition in where?.flattenAnd().orEmpty()) {
        val matched = joinedTables.filter { condition.referencesTable(it) }
        val scope = listOf(rootTable) + matched
        if (matched.size > 1 || !condition.referencesOnly(*scope.toTypedArray())) return null
        matched.firstOrNull()?.let { tableConditions.getValue(it).add(condition) } ?: rootConditions.add(condition)
    }

    val existsOps = joinParts.mapNotNull { part ->
        val joinedTable = part.table as Table
        val conditions = tableConditions.getValue(joinedTable)
        when {
            part.isLateral -> return null
            part.joinType == JoinType.INNER -> Unit
            part.joinType != JoinType.LEFT -> return null
            conditions.isEmpty() -> return@mapNotNull null
            !conditions.all { it.rejectsNull(joinedTable) } -> return null
        }
        val bodyOps = part.conditions.map { (left, right) -> EqOp(left, right) } +
            listOfNotNull(part.additionalConstraint?.invoke()) + conditions
        if (bodyOps.isEmpty() || !bodyOps.all { it.referencesOnly(rootTable, joinedTable) }) return null
        Exists(joinedTable.select(intLiteral(1)).where(bodyOps.compoundAnd()))
    }

    val finalWhere = (rootConditions + existsOps).takeIf { it.isNotEmpty() }?.compoundAnd()
    return Query(rootTable.select(primaryKey).set, finalWhere)
}

/** The parts of a join as Exposed keeps them; its `JoinPart` is internal, so they are read reflectively. */
private data class JoinPartView(
    val joinType: JoinType,
    val table: ColumnSet,
    val conditions: List<Pair<Expression<*>, Expression<*>>>,
    val isLateral: Boolean,
    val additionalConstraint: (() -> Op<Boolean>)?
)

@Suppress("UNCHECKED_CAST")
private fun Join.joinPartsReflect(): List<JoinPartView>? = runCatching {
    val parts = Join::class.java.getDeclaredField("joinParts").apply { isAccessible = true }.get(this) as List<Any>
    parts.map { part ->
        fun field(name: String): Any? = part.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(part)
        JoinPartView(
            joinType = field("joinType") as JoinType,
            table = field("joinPart") as ColumnSet,
            conditions = field("conditions") as List<Pair<Expression<*>, Expression<*>>>,
            isLateral = field("lateral") as Boolean,
            additionalConstraint = field("additionalConstraint") as? () -> Op<Boolean>
        )
    }
}.getOrNull()

/**
 * False for the all-NULL row a LEFT join gives [table] when nothing matches: a comparison, IN list or IS NOT NULL
 * on its column, and AND / OR built from them. Anything else, IS NULL first of all, is not known to be.
 */
@Suppress("UNCHECKED_CAST")
private fun Op<Boolean>.rejectsNull(table: Table): Boolean = when (this) {
    is AndOp -> expressions().orEmpty().any { (it as? Op<Boolean>)?.rejectsNull(table) == true }
    is OrOp -> expressions()?.all { (it as? Op<Boolean>)?.rejectsNull(table) == true } == true
    is IsNotNullOp -> expr.isColumnOf(table)
    is ComparisonOp -> expr1.isColumnOf(table) || expr2.isColumnOf(table)
    // NOT IN an empty list is rendered as TRUE
    is InListOrNotInListBaseOp<*> -> (isInList || list.any()) && (expr as? Expression<*>)?.isColumnOf(table) == true
    else -> false
}

private fun Expression<*>.isColumnOf(table: Table): Boolean = when (this) {
    is Column<*> -> this.table == table
    is LowerCase<*> -> expr.isColumnOf(table)
    is UpperCase<*> -> expr.isColumnOf(table)
    else -> false
}

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

private fun Expression<*>.referencesOnly(vararg tables: Table): Boolean {
    return when (this) {
        Op.TRUE, Op.FALSE -> true
        is Column<*> -> this.table in tables
        is QueryParameter<*> -> true
        is LiteralOp<*> -> true
        is IExpressionAlias<*> -> delegate.referencesOnly(*tables)
        is CompoundBooleanOp -> expressions()?.all { it.referencesOnly(*tables) } == true
        is NotOp<*> -> expr.referencesOnly(*tables)
        is ComparisonOp -> expr1.referencesOnly(*tables) && expr2.referencesOnly(*tables)
        is IsNullOp -> expr.referencesOnly(*tables)
        is IsNotNullOp -> expr.referencesOnly(*tables)
        is IsNotDistinctFromOp -> expression1.referencesOnly(*tables) && expression2.referencesOnly(*tables)
        is IsDistinctFromOp -> expression1.referencesOnly(*tables) && expression2.referencesOnly(*tables)
        is InListOrNotInListBaseOp<*> -> expr.referencesOnly(*tables)
        is Exists -> true
        is NotExists -> true
        is SubQueryOp<*> -> expr.referencesOnly(*tables)
        is LowerCase<*> -> expr.referencesOnly(*tables)
        is UpperCase<*> -> expr.referencesOnly(*tables)
        is Concat -> expr.all { it.referencesOnly(*tables) }
        is Min<*, *> -> expr.referencesOnly(*tables)
        is Max<*, *> -> expr.referencesOnly(*tables)
        is Sum<*> -> expr.referencesOnly(*tables)
        is Count -> expr.referencesOnly(*tables)
        is CustomFunction<*> -> expr.all { it.referencesOnly(*tables) }
        is CustomOperator<*> -> expr1.referencesOnly(*tables) && expr2.referencesOnly(*tables)
        is ModOp<*, *, *> -> expr1.referencesOnly(*tables) && expr2.referencesOnly(*tables)
        is NoOpConversion<*, *> -> expr.referencesOnly(*tables)
        else -> false
    }
}

private fun Any?.referencesOnly(vararg tables: Table): Boolean {
    return when (this) {
        null -> true
        is Expression<*> -> referencesOnly(*tables)
        is Pair<*, *> -> first.referencesOnly(*tables) && second.referencesOnly(*tables)
        is Triple<*, *, *> -> first.referencesOnly(*tables) && second.referencesOnly(*tables) && third.referencesOnly(*tables)
        is Iterable<*> -> all { it.referencesOnly(*tables) }
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
