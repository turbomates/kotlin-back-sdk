package dev.tmsoft.lib.redis

import redis.clients.jedis.JedisPool

class RedisPersistentList(private val pool: JedisPool) {
    fun get(key: String): List<String> {
        return pool.resource.use { it.lrange(key, 0, -1) }
    }

    fun add(key: String, value: String) {
        pool.resource.use { it.lpush(key, value) }
    }

    fun delete(key: String, value: String) {
        pool.resource.use { it.lrem(key, 0, value) }
    }
}
