package dev.tmsoft.lib.exposed.query

import com.google.inject.Inject
import dev.tmsoft.lib.exposed.TransactionManager

class QueryExecutor @Inject constructor(private val transactional: TransactionManager) {
    suspend fun <T> execute(queryObject: QueryObject<T>): T {
        return transactional { queryObject.getData() }
    }

    suspend fun <T> ttl(queryObject: CachedQueryObject<T>): Long {
        return transactional.readOnlyTransaction { queryObject.ttl() }
    }
}
