package dev.tmsoft.lib.redis

import dev.tmsoft.lib.logger.logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RedisPersistentHash(
    private val access: Access,
    private val prefix: String? = null,
    val serializer: Json = Json,
) {
    private val logger = logger()
    fun get(key: String, field: String): String? {
        logger.debug("Get value for key: {}, field: {}", key, field)
        return access.hget(prefix?.let { "$prefix:$key" } ?: key, field)
    }

    inline fun <reified T> get(key: String, field: String): T? {
        val json: String? = get(key, field)
        return json?.let { serializer.decodeFromString(json) }
    }

    fun set(key: String, field: String, value: String) {
        logger.debug("Set value for key: {}, field: {}, value: {}", key, field, value)
        access.hset(prefix?.let { "$prefix:$key" } ?: key, field, value)
    }

    inline fun <reified T> set(key: String, field: String, value: T) {
        set(key, field, serializer.encodeToString(value))
    }

    fun exists(key: String): Boolean {
        logger.debug("Check if key exists: {}", key)
        return access.exists(prefix?.let { "$prefix:$key" } ?: key)
    }

    fun remove(key: String) {
        logger.debug("Delete for key: {}", key)
        access.remove(prefix?.let { "$prefix:$key" } ?: key)
    }
}
