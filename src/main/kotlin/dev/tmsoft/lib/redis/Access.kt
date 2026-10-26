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

    fun zadd(key: String, score: Double, member: String)
    fun zadd(key: String, members: Map<String, Double>)
    fun zcount(key: String, minScore: Double, maxScore: Double): Long
    fun zscore(key: String, member: String): Double?
    fun zincrby(key: String, increment: Double, member: String): Double
    fun zrangeByScore(key: String, minScore: Double, maxScore: Double): List<String>
    fun zrem(key: String, vararg members: String): Long
    fun zremrangeByScore(key: String, minScore: Double, maxScore: Double): Long
    fun expire(key: String, seconds: Long): Long

    fun exists(key: String): Boolean
    fun remove(key: String)
}
