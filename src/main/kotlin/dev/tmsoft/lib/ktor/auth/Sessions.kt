package dev.tmsoft.lib.ktor.auth

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import io.ktor.sessions.SessionSerializer as KtorSessionSerializer

@Serializable
data class Session(
    val principal: Principal? = null,
    val attributes: MutableMap<String, String> = mutableMapOf(),
    val ttl: Int = 3600
)

@Serializable
@Polymorphic
abstract class Principal : io.ktor.auth.Principal

class SessionSerializer(module: SerializersModule) : KtorSessionSerializer<Session> {
    val serializer: Json = Json { serializersModule = module }

    override fun serialize(session: Session): String = serializer.encodeToString(session)
    override fun deserialize(text: String): Session = serializer.decodeFromString(text)
}
