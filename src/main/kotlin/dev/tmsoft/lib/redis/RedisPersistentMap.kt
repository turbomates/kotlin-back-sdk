package dev.tmsoft.lib.redis

import dev.tmsoft.lib.logger.logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisPool

class RedisPersistentMap(private val pool: JedisPool, private val prefix: String? = null) {
    private val logger = logger()
    val serializer: Json = Json

    fun get(key: String): String? {
        logger.debug("Get value for key: {}", key)
        return pool.resource.use { it[prefix?.let { "$prefix:$key" } ?: key] }
    }

    inline fun <reified T> get(key: String): T? {
        val json: String? = get(key)
        return json?.let { serializer.decodeFromString<T>(it) }
    }

    fun set(key: String, value: String) {
        logger.debug("set value for key: {}, value: {}", key, value)
        pool.resource.use { it[prefix?.let { "$prefix:$key" } ?: key] = value }
    }

    inline fun <reified T> set(key: String, value: T) {
        val json = serializer.encodeToString(value)
        set(key, json)
    }

    fun set(key: String, ttl: Long, value: String) {
        logger.debug("set value for key: {}, ttl: {}, value: {}", key, ttl, value)
        pool.resource.use { it.setex(prefix?.let { "$prefix:$key" } ?: key, ttl, value) }
    }

    inline fun <reified T> set(key: String, ttl: Long, value: T) {
        set(key, ttl, serializer.encodeToString(value))
    }

    fun remove(key: String) {
        pool.resource.use { it.del(prefix?.let { "$prefix:$key" } ?: key) }
    }
}
