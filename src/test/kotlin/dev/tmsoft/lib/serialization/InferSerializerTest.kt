package dev.tmsoft.lib.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.jupiter.api.Test

class InferSerializerTest {
    @Test
    fun `single value`() {
        val test = listOf(BonusAutomation("1", "1"), NotificationAutomation("2", "2"))
        val json = Json { serializersModule = Automation.module }
        json.encodeToString(resolveSerializer(test), test)
    }
}

@Serializable
data class BonusAutomation(
    override val id: String,
    override val groupId: String,
) : Automation

@Serializable
data class NotificationAutomation(
    override val id: String,
    override val groupId: String,
) : Automation

@Serializable
sealed interface Automation {
    val id: String
    val groupId: String

    companion object {
        val module = SerializersModule {
            polymorphic(Automation::class) {
                subclass(BonusAutomation::class)
                subclass(NotificationAutomation::class)
            }
        }
    }
}
