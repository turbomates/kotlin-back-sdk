package dev.tmsoft.lib.redis

import dev.tmsoft.lib.logger.logger
import redis.clients.jedis.JedisPool

class RedisPersistentList(private val pool: JedisPool, private val prefix: String? = null) {
    private val logger = logger()
    fun get(key: String): List<String> {
        logger.debug("Get value for key: {}", key)
        return pool.resource.use { it.lrange(prefix?.let { "$prefix:$key" } ?: key, 0, -1) }
    }

    fun add(key: String, value: String) {
        logger.debug("Add value for key: {}, value: {}", key, value)
        pool.resource.use { it.lpush(prefix?.let { "$prefix:$key" } ?: key, value) }
    }

    fun delete(key: String, value: String) {
        logger.debug("Delete value for key: {}, value: {}", key, value)
        pool.resource.use { it.lrem(prefix?.let { "$prefix:$key" } ?: key, 0, value) }
    }
}
