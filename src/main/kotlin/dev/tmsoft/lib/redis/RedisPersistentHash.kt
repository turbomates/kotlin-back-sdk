package dev.tmsoft.lib.redis

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisPool

class RedisPersistentHash(private val pool: JedisPool, val serializer: Json = Json, private val prefix: String? = null) {

    fun get(key: String, field: String): String? {
        return pool.resource.use { it.hget(prefix?.let { "$prefix:$key" } ?: key, field) }
    }

    inline fun <reified T> get(key: String, field: String): T? {
        val json: String? = get(key, field)
        return json?.let { serializer.decodeFromString(json) }
    }

    fun set(key: String, field: String, value: String) {
        pool.resource.use { it.hset(prefix?.let { "$prefix:$key" } ?: key, field, value) }
    }

    inline fun <reified T> set(key: String, field: String, value: T) {
        set(key, field, serializer.encodeToString(value))
    }

    fun remove(key: String) {
        pool.resource.use { it.del(prefix?.let { "$prefix:$key" } ?: key) }
    }
}
