@file:Suppress("NestedBlockDepth", "SpreadOperator")

package dev.tmsoft.lib.query.paging

import dev.tmsoft.lib.serialization.elementSerializer
import kotlinx.coroutines.async
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
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.ops.SingleValueInListOp

suspend fun <T> Query.toContinuousList(
    page: PagingParameters,
    effector: ResultRow.() -> T,
    sortingParameters: List<SortingParameter>? = null,
    includeCount: Boolean = false
): ContinuousList<T> {
    return toContinuousListBuilder(page, sortingParameters, includeCount) { this.map { effector(it) } }
}

@JvmName("toContinuousListIterable")
suspend fun <T> Query.toContinuousList(
    page: PagingParameters,
    effector: Iterable<ResultRow>.() -> List<T>,
    sortingParameters: List<SortingParameter>? = null,
    includeCount: Boolean = false
): ContinuousList<T> {
    return toContinuousListBuilder(page, sortingParameters, includeCount) { effector() }
}

@Suppress("SpreadOperator")
suspend fun <T> Query.toContinuousListBuilder(
    page: PagingParameters,
    sortingParameters: List<SortingParameter>? = null,
    includeCount: Boolean = false,
    effector: Query.() -> List<T>
): ContinuousList<T> = coroutineScope {
    val countQuery = copy()

    val count = if (includeCount) {
        countQuery.count()
    } else {
        null
    }

    sortedWith(sortingParameters)

    if (targets.count() > 1) {
        val rootTable = targets.first()
        if (rootTable.primaryKey != null) {
            modifyWhereIn(rootTable.primaryKey!!.columns.first(), page.pageSize + 1, page.offset)
        }
    } else {
        limit(page.pageSize + 1, page.offset)
    }

    var result = effector()
    var hasMore = false
    if (result.count() > page.pageSize) {
        hasMore = result.count() > page.pageSize
        result = result.dropLast(1)
    }
    ContinuousList(result, page.pageSize, page.currentPage, hasMore, count)
}

private fun Query.sortedWith(sortingParameters: List<SortingParameter>? = null): Query {
    return apply {
        if (sortingParameters != null) {
            val columns = targets.map { it.columns }.flatten()
            sortingParameters
                .associate { sortingParameter ->
                    val column = columns.find { sortingParameter.name == it.name }
                        ?: throw IllegalArgumentException("Unknown sorting parameter: ${sortingParameter.name}")
                    column to sortingParameter.sortOrder
                }
                .toList().toTypedArray()
                .run { if (isNotEmpty()) orderBy(*this) }
        }
    }
}

private fun <T> Query.modifyWhereIn(column: Column<T>, limit: Int, offset: Long): Query {
    val query = copy()
    query.limit(limit, offset)
    val ids =
        query.adjustSlice { slice(listOf(column) + orderByExpressions.map { it.first }) }
            .withDistinct()
            .map { it[column] }
    return adjustWhere { SingleValueInListOp(column, ids) }
}

class ContinuousList<T>(
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
