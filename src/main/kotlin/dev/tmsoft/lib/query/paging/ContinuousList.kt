@file:Suppress("NestedBlockDepth", "SpreadOperator")

package dev.tmsoft.lib.query.paging

import dev.tmsoft.lib.exposed.sql.RowNumberFunction
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
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.ExpressionWithColumnTypeAlias
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.Min
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.ops.SingleValueInListOp

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
        if (rootTable.primaryKey != null) {
            val primaryKey = rootTable.primaryKey!!.columns.first()
            val primaryKeyAlias = primaryKey.alias("uniq_field_id")
            val countQuery = Query(set, where)
            adjustWhereIn(primaryKey, sortingParameters, page.pageSize + 1, page.offset)
            if (includeCount) {
                count = countQuery.distinctSubQuery(primaryKeyAlias, sortingParameters).count()
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
    if (result.count() > page.pageSize) {
        hasMore = result.count() > page.pageSize
        result = result.dropLast(1)
    }
    ContinuousList(result, page.pageSize, page.currentPage, hasMore, count)
}

fun <T> Query.adjustWhereIn(
    primaryKey: ExpressionWithColumnType<T>,
    sortingParameters: List<SortingParameter>,
    limit: Int,
    offset: Long
) {
    val primaryKeyAlias = primaryKey.alias("uniq_field_id")
    val ids = distinctSubQuery(primaryKeyAlias, sortingParameters)
        .limit(limit)
        .offset(offset)
        .map { it[primaryKeyAlias.aliasOnlyExpression()] }
    adjustWhere { SingleValueInListOp(primaryKey, ids) }
    sortedWith(sortingParameters)
}

private fun Query.sortedWith(sortingParameters: List<SortingParameter>): Query {
    return apply {
        buildSortingParameters(sortingParameters).run { if (isNotEmpty()) orderBy(*this) }
    }
}

fun Query.buildSortingParameters(sortingParameters: List<SortingParameter>): Array<Pair<Expression<*>, SortOrder>> {
    val columns = targets.map { it.columns }.flatten()
    return sortingParameters
        .associate { sortingParameter ->
            val column = columns.find { sortingParameter.name == it.name }
                ?: throw IllegalArgumentException("Unknown sorting parameter: ${sortingParameter.name}")
            column to sortingParameter.sortOrder
        }
        .toList().toTypedArray()
}

//SELECT %s AS dctrn_count FROM (SELECT DISTINCT %s FROM (%s) dctrn_result) dctrn_table
private fun <T> Query.distinctSubQuery(
    column: ExpressionWithColumnTypeAlias<T>,
    sortingParameters: List<SortingParameter>
): Query {
    val query = Query(set, where)
    val rowNumber =
        RowNumberFunction(buildSortingParameters(sortingParameters) + orderByExpressions).alias("row_number")
    val subQuery = query.adjustSelect { select(listOf(column) + rowNumber) }
        .withDistinct().alias("subquery")
    val minColumn = Min(rowNumber.aliasOnlyExpression(), LongColumnType()).alias("min_column")
    return subQuery
        .select(
            listOf<Expression<*>>(
                column.aliasOnlyExpression(),
                minColumn
            )
        )
        .withDistinct()
        .groupBy(column.aliasOnlyExpression())
        .orderBy(minColumn)
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
