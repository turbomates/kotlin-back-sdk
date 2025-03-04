package dev.tmsoft.lib.redis

import dev.tmsoft.lib.logger.logger

class RedisPersistentList(private val access: Access, private val prefix: String? = null) {
    private val logger = logger()
    fun get(key: String): List<String> {
        logger.debug("Get value for key: {}", key)
        return access.lrange(prefix?.let { "$prefix:$key" } ?: key)
    }

    fun add(key: String, value: String) {
        logger.debug("Add value for key: {}, value: {}", key, value)
        access.lpush(prefix?.let { "$prefix:$key" } ?: key, value)
    }

    fun delete(key: String, value: String) {
        logger.debug("Delete value for key: {}, value: {}", key, value)
        access.lrem(prefix?.let { "$prefix:$key" } ?: key, value)
    }
}
