package dev.tmsoft.lib.query.paging

import dev.tmsoft.lib.serialization.elementSerializer
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

fun <T> Query.toContinuousList(
    page: PagingParameters,
    effector: ResultRow.() -> T,
    sortingParameters: List<SortingParameter>? = null
    ): ContinuousList<T> {
    return toContinuousListBuilder(page, sortingParameters) { this.map { effector(it) } }
}

@JvmName("toContinuousListIterable")
fun <T> Query.toContinuousList(
    page: PagingParameters,
    effector: Iterable<ResultRow>.() -> List<T>,
    sortingParameters: List<SortingParameter>? = null
    ): ContinuousList<T> {
    return toContinuousListBuilder(page, sortingParameters) { effector() }
}

@Suppress("SpreadOperator")
fun <T> Query.toContinuousListBuilder(
    page: PagingParameters,
    sortingParameters: List<SortingParameter>? = null,
    effector: Query.() -> List<T>
): ContinuousList<T> {
    if (targets.count() > 1) {
        val rootTable = targets.first()
        if (rootTable.primaryKey != null) {
            modifyWhereIn(rootTable.primaryKey!!.columns.first(), page.pageSize + 1, page.offset)
        }
    } else {
        limit(page.pageSize + 1, page.offset)
    }

    val columns = targets.map { it.columns }.flatten()
    val orders = sortingParameters
        ?.associate { sortingParameter ->
            val column = columns.find { sortingParameter.name == it.name }
                ?: throw IllegalArgumentException("Unknown sorting parameter: ${sortingParameter.name}")

            column to sortingParameter.sortOrder
        }
        ?.toList()
        ?.toTypedArray() ?: emptyArray()

    var result = effector(orderBy(*orders))
    var hasMore = false
    if (result.count() > page.pageSize) {
        hasMore = result.count() > page.pageSize
        result = result.dropLast(1)
    }
    return ContinuousList(result, page.pageSize, page.currentPage, hasMore)
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
    val hasMore: Boolean = false
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

        val tree = JsonObject(
            mapOf(
                "pageSize" to JsonPrimitive(value.pageSize),
                "page" to JsonPrimitive(value.currentPage),
                "hasMore" to JsonPrimitive(value.hasMore),
                "data" to output.json.parseToJsonElement(encoded)
            )
        )
        output.encodeJsonElement(tree)
    }

    override fun deserialize(decoder: Decoder): ContinuousList<*> {
        throw NotImplementedError()
    }
}
