package dev.tmsoft.lib.saga

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.jupiter.api.Test

class SagaSerializerTest {
    @Test
    fun serialize() {
        val test = TestSagaSerialization("test", 1)
        val serializer = Json {
            encodeDefaults = true
        }
        val result = serializer.encodeToString(SagaSerializer, test)
        Assert.assertTrue(result.contains("dev.tmsoft.lib.saga.TestSagaSerialization"))
        Assert.assertTrue(result.contains("test"))
        Assert.assertTrue(result.contains("1"))
    }

    @Test
    fun deserialize() {
        val test = TestSagaSerialization("test", 1)
        val serializer = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        val result = serializer.decodeFromString(
            SagaSerializer,
            serializer.encodeToString(SagaSerializer, test)
        )
        Assert.assertTrue(result is TestSagaSerialization)
    }
}

@Serializable
data class TestSagaSerialization(val id: String, val data: Int) : Saga.Data {
    override val key: Saga.Key get() = Companion

    companion object : Saga.Key {
        override val name: String = "test"
    }
}
