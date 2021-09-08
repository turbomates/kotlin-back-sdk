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
inline fun <T> Query.fold(
    rootTable: IdTable<*>,
    rootMapper: ResultRow.() -> T,
    dependenciesMappers: MutableMap<IdTable<*>, ResultRow.(T) -> Unit>?
): List<T> {
    checkCorrectRootTable(rootTable)
    val dependencies: MutableMap<IdTable<*>, MutableMap<String, MutableSet<String>>> = mutableMapOf()
    val roots: MutableMap<String, T> = mutableMapOf()
    forEach { resultRow ->
        if(resultRow.getOrNull(rootTable.id) != null) {
            val root: T
            val rootKey = resultRow[rootTable.id].value.toString()
            if (roots.contains(rootKey)) {
                root = roots[rootKey]!!
            } else {
                root = rootMapper(resultRow)
                roots[rootKey] = root
            }
            if (dependenciesMappers != null) {
                for ((dependencyTable, dependencyMapper) in dependenciesMappers) {
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
    }
    return roots.values.toList()
}

fun Query.checkCorrectRootTable(rootTable: IdTable<*>) {
    val tablesInQuery = (set as Join).targetTables().map { it.tableName }
    if(!tablesInQuery.contains(rootTable.tableName)) {
        throw IllegalArgumentException("The corresponding root table is not present in the resulting query")
    }
}