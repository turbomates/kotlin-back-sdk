package dev.tmsoft.lib.redis

import redis.clients.jedis.JedisPool

class RedisPersistentList(private val pool: JedisPool, private val prefix: String? = null) {
    fun get(key: String): List<String> {
        return pool.resource.use { it.lrange(prefix?.let { "$prefix:$key" } ?: key, 0, -1) }
    }

    fun add(key: String, value: String) {
        pool.resource.use { it.lpush(prefix?.let { "$prefix:$key" } ?: key, value) }
    }

    fun delete(key: String, value: String) {
        pool.resource.use { it.lrem(prefix?.let { "$prefix:$key" } ?: key, 0, value) }
    }
}
