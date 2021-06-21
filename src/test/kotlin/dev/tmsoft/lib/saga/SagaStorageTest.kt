package dev.tmsoft.lib.saga

import dev.tmsoft.lib.exposed.rollbackTransaction
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SchemaUtils
import org.junit.Assert
import org.junit.jupiter.api.Test

class SagaStorageTest {
    @Test
    fun `save saga`() {
        rollbackTransaction {
            SchemaUtils.create(SagaTable)
            val testData = Saga(SagaId("test-id", TestSagaStorage), TestSagaStorage("test", 1))
            val storage = SagaStorage()
            storage.save(testData)
            val result: Saga<TestSagaStorage>? = storage.findById(SagaId("test-id", TestSagaStorage))
            Assert.assertEquals(testData.id, result!!.id)
        }
    }
}

@Serializable
data class TestSagaStorage(val id: String, val data: Int) : Saga.Data {
    override val key: Saga.Key get() = Companion

    companion object : Saga.Key {
        override val name: String = "test"
    }
}
