package dev.tmsoft.lib.query

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.targetTables

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
        if (resultRow.getOrNull(rootTable.id) != null) {
            val root: T
            val rootKey = resultRow[rootTable.id].value.toString()
            if (roots.contains(rootKey)) {
                root = roots.getValue(rootKey)
            } else {
                root = rootMapper(resultRow)
                roots[rootKey] = root
            }
            for ((dependencyTable, dependencyMapper) in dependenciesMappers) {
                checkCorrectMappingTable(dependencyTable)
                if (resultRow.getOrNull(dependencyTable.id) != null) {
                    val uniqueDependencyKeys = dependencies[dependencyTable]?.get(rootKey)
                    val currentDependencyKey = resultRow[dependencyTable.id].value.toString()
                    if ((uniqueDependencyKeys != null && !uniqueDependencyKeys.contains(currentDependencyKey)) || uniqueDependencyKeys == null) {
                        dependencyMapper(resultRow, root)
                    }
                    if (uniqueDependencyKeys == null) {
                        dependencies[dependencyTable] = mutableMapOf()
                        dependencies[dependencyTable]?.put(rootKey, mutableSetOf(currentDependencyKey))
                    } else {
                        dependencies[dependencyTable]?.get(rootKey)?.add(currentDependencyKey)
                    }
                }
            }
        }
    }
    return roots.values.toList()
}

private fun Query.checkCorrectMappingTable(table: IdTable<*>) {
    val tablesInQuery = (set as Join).targetTables().map { it.tableName }
    if (!tablesInQuery.contains(table.tableName)) {
        throw IllegalArgumentException("The corresponding table is not present in the resulting query")
    }
}
