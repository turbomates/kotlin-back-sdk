package dev.tmsoft.lib.redis

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisPool

class RedisPersistentMap(private val pool: JedisPool) {
    val serializer: Json = Json

    fun get(key: String): String? {
        return pool.resource.use { it[key] }
    }

    inline fun <reified T> get(key: String): T? {
        val json: String? = get(key)
        return json?.let { serializer.decodeFromString<T>(it) }
    }

    fun set(key: String, value: String) {
        pool.resource.use { it[key] = value }
    }

    inline fun <reified T> set(key: String, value: T) {
        val json = serializer.encodeToString(value)
        set(key, json)
    }

    fun set(key: String, ttl: Long, value: String) {
        pool.resource.use { it.setex(key, ttl, value) }
    }

    inline fun <reified T> set(key: String, ttl: Long, value: T) {
        set(key, ttl, serializer.encodeToString(value))
    }

    fun remove(key: String) {
        pool.resource.use { it.del(key) }
    }
}
