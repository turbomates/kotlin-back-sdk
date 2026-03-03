package dev.tmsoft.lib.serialization

import java.math.BigDecimal
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BigDecimalSerializerTest {

    @Test
    fun `deserialize bigdecimal from json string`() {
        val json = Json
        val payload = """{"code":"test","name":"test","description":"test","maxContribution":"1","internalCap":"1.1","isActive":true}"""

        val result = json.decodeFromString(Campaign.serializer(), payload)

        assertEquals(BigDecimal("1"), result.maxContribution)
        assertEquals(BigDecimal("1.1"), result.internalCap)
    }

    @Serializable
    data class Campaign(
        val code: String,
        val name: String,
        val description: String,
        @Serializable(with = BigDecimalSerializer::class)
        val maxContribution: BigDecimal,
        @Serializable(with = BigDecimalSerializer::class)
        val internalCap: BigDecimal,
        val isActive: Boolean,
    )
}
