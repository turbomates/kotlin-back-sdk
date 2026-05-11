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
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
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
        val rootTable = targets.first()
        val primaryKey = rootTable.primaryKey?.columns?.first()
        if (primaryKey != null) {
            val countQuery = Query(set, where)
            adjustWhereIn(primaryKey, sortingParameters, page.pageSize + 1, page.offset)
            if (includeCount) {
                count = countQuery.distinctCount(primaryKey)
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
    val (idsQuery, idExpression) = distinctSubQuery(primaryKey, sortingParameters)
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
