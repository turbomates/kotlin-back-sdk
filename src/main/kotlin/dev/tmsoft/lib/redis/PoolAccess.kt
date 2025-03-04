package dev.tmsoft.lib.redis

import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.ScanParams

@Suppress("TooManyFunctions")
class PoolAccess(private val pool: JedisPool) : Access {
    override fun get(key: String): String? {
        return pool.resource.use { it.get(key) }
    }

    override fun hget(key: String, field: String): String? {
        return pool.resource.use { it.hget(key, field) }
    }

    override fun hset(key: String, field: String, value: String) {
        pool.resource.use { it.hset(key, field, value) }
    }

    override fun lrange(key: String): List<String> {
        pool.resource.use { return it.lrange(key, 0, -1) }
    }

    override fun lpush(key: String, value: String) {
        pool.resource.use { it.lpush(key, value) }
    }

    override fun lrem(key: String, value: String) {
        pool.resource.use { it.lrem(key, 0, value) }
    }

    override fun sadd(key: String, value: String) {
        pool.resource.use { it.sadd(key, value) }
    }

    override fun smembers(key: String): Set<String> {
        return pool.resource.use { it.smembers(key) }
    }

    override fun scard(key: String): Long {
        return pool.resource.use { it.scard(key) }
    }

    override fun srem(key: String, vararg values: String) {
        return pool.resource.use { it.srem(key, *values) }
    }

    override fun findKeys(
        prefix: String,
        count: Int
    ): List<String> {
        return pool.resource.use { it.scan("0", ScanParams().match("$prefix*").count(count)).result }
    }

    override fun findByPrefix(
        prefix: String,
        count: Int
    ): List<String> {
        val keys = findKeys(prefix, count)
        return keys.mapNotNull { get(it) }
    }

    override fun set(
        key: String,
        value: String
    ) {
        pool.resource.use { it.set(key, value) }
    }

    override fun set(
        key: String,
        value: String,
        ttl: Long
    ) {
        pool.resource.use { it.setex(key, ttl, value) }
    }

    override fun exists(key: String): Boolean {
        return pool.resource.use { it.exists(key) }
    }

    override fun remove(key: String) {
        pool.resource.use { it.del(key) }
    }
}
