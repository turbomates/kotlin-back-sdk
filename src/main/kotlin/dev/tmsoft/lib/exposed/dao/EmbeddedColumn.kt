package dev.tmsoft.lib.exposed.dao

import kotlin.reflect.KProperty
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Column as ExposedColumn
import org.jetbrains.exposed.sql.CompositeColumn as ExposedCompositeColumn

abstract class EmbeddableColumn<T : Embedded>(private val table: Table, private val prefix: String = "") :
    ExposedCompositeColumn<T>() {
    private val columns: MutableMap<ExposedColumn<*>, PrimitiveColumn<*>> = mutableMapOf()
    private val embeddableColumns: MutableMap<EmbeddableColumn<Embedded>, CompositeColumn<*>> = mutableMapOf()
    override fun getRealColumns(): List<ExposedColumn<*>> {
        val result: MutableList<ExposedColumn<*>> = mutableListOf()
        result.addAll(columns.keys.toList())
        embeddableColumns.forEach {
            result.addAll(it.key.getRealColumns())
        }
        return result.toList()
    }

    @Suppress("UNCHECKED_CAST")
    override fun getRealColumnsWithValues(compositeValue: T): Map<ExposedColumn<*>, Any?> {
        val result = mutableMapOf<ExposedColumn<*>, Any?>()
        columns.forEach {
            result[it.key] = compositeValue.map[it.value]
        }
        embeddableColumns.forEach {
            if (compositeValue.map.containsKey(it.value)) {
                result += it.key.getRealColumnsWithValues(compositeValue.map[it.value] as Embedded)
            }
        }
        return result
    }

    override fun restoreValueFromParts(parts: Map<ExposedColumn<*>, Any?>): T {
        val result = mutableMapOf<Column<*>, Any?>()
        columns.forEach {
            result[it.value] = rawToColumnValue(parts[it.key], it.key)
        }
        embeddableColumns.forEach {
            result[it.value] = it.key.restoreValueFromParts(parts)
        }
        val instance = instance(result)
        instance.map = result
        return instance
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> rawToColumnValue(raw: T?, c: ExposedColumn<*>): T {
        return (if (raw != null) c.columnType.valueFromDB(raw) else null) as T
    }

    fun <TColumn> column(column: PrimitiveColumn<TColumn>): ExposedColumn<TColumn> {
        val exposedColumn = column.block(table, prefix)
        columns[exposedColumn] = column
        return exposedColumn
    }

    @Suppress("UNCHECKED_CAST")
    fun <TColumn : EmbeddableColumn<out Embedded>> column(column: CompositeColumn<TColumn>): TColumn {
        val exposedColumn = column.block(table, prefix)
        embeddableColumns[exposedColumn as EmbeddableColumn<Embedded>] = column
        return exposedColumn
    }

    abstract fun instance(parts: Map<Column<*>, Any?>): T
    override fun hashCode(): Int {
        val result = table.hashCode()
        return getRealColumns().fold(result) { acc, column ->
            31 * acc + column.hashCode()
        }
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun toString(): String {
        return getRealColumns().joinToString(",") {
            "${table.tableName}.${it.name}"
        }
    }
}

abstract class EmbeddedTable {
    fun <T> column(block: Table.(prefix: String) -> ExposedColumn<T>): PrimitiveColumn<T> {
        return PrimitiveColumn(block)
    }

    fun <T : EmbeddableColumn<*>> compositeColumn(block: Table.(prefix: String) -> T): CompositeColumn<T> {
        return CompositeColumn(block)
    }
}

sealed class Column<T>(val block: Table.(prefix: String) -> T)

class PrimitiveColumn<T>(block: Table.(prefix: String) -> ExposedColumn<T>) : Column<ExposedColumn<T>>(block)

class CompositeColumn<T : EmbeddableColumn<*>>(block: Table.(prefix: String) -> T) : Column<T>(block)

abstract class Embedded {
    internal var map: MutableMap<Column<*>, Any?> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    operator fun <D : Embedded, T : EmbeddableColumn<D>> CompositeColumn<T>.getValue(
        o: Embedded,
        desc: KProperty<*>
    ): D {
        return o.map[this] as D
    }

    operator fun <D : Embedded?, T : EmbeddableColumn<D>> CompositeColumn<T>.setValue(
        o: Embedded,
        desc: KProperty<*>,
        value: D
    ) {
        o.map[this] = value
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> PrimitiveColumn<T>.getValue(o: Embedded, desc: KProperty<*>): T {
        return o.map[this] as T
    }

    operator fun <T> PrimitiveColumn<T>.setValue(o: Embedded, desc: KProperty<*>, value: T) {
        o.map[this] = value
    }
}
