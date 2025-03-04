package dev.tmsoft.lib.redis

import dev.tmsoft.lib.logger.logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RedisPersistentMap(private val access: Access, private val prefix: String? = null) {
    private val logger = logger()
    val serializer: Json = Json

    fun get(key: String): String? {
        logger.debug("Get value for key: {}", key)
        return access.get(prefix?.let { "$prefix:$key" } ?: key)
    }

    inline fun <reified T> get(key: String): T? {
        val json: String? = get(key)
        return json?.let { serializer.decodeFromString<T>(it) }
    }

    fun set(key: String, value: String) {
        logger.debug("set value for key: {}, value: {}", key, value)
        access.set(prefix?.let { "$prefix:$key" } ?: key, value)
    }

    inline fun <reified T> set(key: String, value: T) {
        val json = serializer.encodeToString(value)
        set(key, json)
    }

    fun set(key: String, ttl: Long, value: String) {
        logger.debug("set value for key: {}, ttl: {}, value: {}", key, ttl, value)
        access.set(prefix?.let { "$prefix:$key" } ?: key, value, ttl)
    }

    inline fun <reified T> set(key: String, ttl: Long, value: T) {
        set(key, ttl, serializer.encodeToString(value))
    }

    fun exists(key: String): Boolean {
        logger.debug("Check if key exists: {}", key)
        return access.exists(prefix?.let { "$prefix:$key" } ?: key)
    }

    fun remove(key: String) {
        access.remove(prefix?.let { "$prefix:$key" } ?: key)
    }
}
