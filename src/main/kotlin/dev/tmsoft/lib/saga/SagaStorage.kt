package dev.tmsoft.lib.saga

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

class SagaStorage {
    @Suppress("UNCHECKED_CAST")
    fun <T : Saga.Data> findById(id: SagaId): Saga<T>? {
        return SagaTable.select { (SagaTable.id eq id.id) and (SagaTable.name eq id.key.name) }.singleOrNull()
            ?.let { val data = it[SagaTable.metadata] as T; Saga(SagaId(it[SagaTable.id].value, data.key), data) }
    }

    fun <T : Saga.Data> save(saga: Saga<T>) {
        SagaTable.insert {
            it[id] = saga.id.id
            it[name] = saga.id.key.name
            it[timeout] = saga.timeout
            it[metadata] = saga.data
            it[status] = saga.status
        }
    }
}
