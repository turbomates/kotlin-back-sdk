package dev.tmsoft.lib.exposed.query

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.targetTables
import org.jetbrains.exposed.v1.jdbc.Query

/**
 * Function used to build nested entities as a result of executing an SQL expression
 *
 * @param rootTable root table IdTable instance
 * @param rootMapper root table mapping expression
 * @param dependenciesMappers key - IdTable instance of the entity you are trying to map, value - mapping expression
 */
fun <T> Query.fold(
    rootTable: IdTable<*>,
    rootMapper: ResultRow.() -> T,
    dependenciesMappers: Map<IdTable<*>, ResultRow.(T) -> T> = emptyMap()
): List<T> {
    checkCorrectMappingTable(rootTable)
    val dependencies: MutableMap<IdTable<*>, MutableMap<String, MutableSet<String>>> = mutableMapOf()
    val roots: MutableMap<String, T> = mutableMapOf()
    forEach { resultRow ->
        resultRow.callIfExist(rootTable.id) {
            val rootKey = this[rootTable.id].value.toString()
            val root = roots.getOrDefault(rootKey, rootMapper(this))
            roots[rootKey] = root
            for ((dependencyTable, dependencyMapper) in dependenciesMappers) {
                checkCorrectMappingTable(dependencyTable)
                callIfExist(dependencyTable.id) {
                    val table = dependencies.getOrDefault(dependencyTable, mutableMapOf())
                    val uniqueDependencyKeys = table.getOrDefault(rootKey, mutableSetOf())
                    val currentDependencyKey = this[dependencyTable.id].value.toString()
                    if (!uniqueDependencyKeys.contains(currentDependencyKey)) {
                        roots[rootKey] = dependencyMapper(this, root)
                    }
                    uniqueDependencyKeys.add(currentDependencyKey)
                    table[rootKey] = uniqueDependencyKeys
                    dependencies[dependencyTable] = table
                }
            }
        }
    }
    return roots.values.toList()
}

private fun ResultRow.callIfExist(column: Expression<*>, callback: ResultRow.() -> Unit) {
    if (getOrNull(column) != null) {
        callback()
    }
}

private fun Query.checkCorrectMappingTable(table: IdTable<*>) {
    val joinedTablesNames = (set as? Join)?.run { targetTables().map { it.tableName } }
    val mainTableName = (set as? Table)?.tableName
    if (mainTableName != table.tableName && joinedTablesNames != null && !joinedTablesNames.contains(table.tableName)) {
        throw IllegalArgumentException("The corresponding table is not present in the resulting query")
    }
}
