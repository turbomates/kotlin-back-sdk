package dev.tmsoft.lib.redis


@Suppress("TooManyFunctions")
interface Access {
    fun get(key: String): String?
    fun hget(key: String, field: String): String?
    fun hset(key: String, field: String, value: String)
    fun lrange(key: String): List<String>
    fun lpush(key: String, value: String)
    fun lrem(key: String, value: String)
    fun sadd(key: String, value: String)
    fun smembers(key: String): Set<String>
    fun scard(key: String): Long
    fun srem(key: String, vararg values: String)
    fun findKeys(
        prefix: String,
        count: Int
    ): List<String>

    fun findByPrefix(
        prefix: String,
        count: Int
    ): List<String>

    fun set(
        key: String,
        value: String
    )

    fun set(
        key: String,
        value: String,
        ttl: Long
    )

    fun exists(key: String): Boolean
    fun remove(key: String)
}
