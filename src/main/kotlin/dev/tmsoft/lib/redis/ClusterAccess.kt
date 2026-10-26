package dev.tmsoft.lib.redis

import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.params.ScanParams


@Suppress("TooManyFunctions")
class ClusterAccess(private val cluster: JedisCluster) : Access {
    override fun get(key: String): String? {
        return cluster[key]
    }

    override fun hget(key: String, field: String): String? {
        return cluster.hget(key, field)
    }

    override fun hset(key: String, field: String, value: String) {
        cluster.hset(key, field, value)
    }

    override fun lrange(key: String): List<String> {
        return cluster.lrange(key, 0, -1)
    }

    override fun lpush(key: String, value: String) {
        cluster.lpush(key, value)
    }

    override fun lrem(key: String, value: String) {
        cluster.lrem(key, 0, value)
    }

    override fun sadd(key: String, value: String) {
        cluster.sadd(key, value)
    }

    override fun smembers(key: String): Set<String> {
        return cluster.smembers(key)
    }

    override fun scard(key: String): Long {
        return cluster.scard(key)
    }

    override fun srem(key: String, vararg values: String) {
        cluster.srem(key, *values)
    }

    override fun findKeys(
        prefix: String,
        count: Int
    ): List<String> {
        return cluster.clusterNodes.flatMap { (_, nodePool) ->
            nodePool.resource.use {
                Jedis(it).scan("0", ScanParams().match("$prefix*").count(count)).result
            }
        }
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
        value: String,
        ttl: Long
    ) {
        cluster.setex(key, ttl, value)
    }

    override fun zadd(key: String, score: Double, member: String) {
        cluster.zadd(key, score, member)
    }

    override fun zadd(key: String, members: Map<String, Double>) {
        cluster.zadd(key, members)
    }

    override fun zcount(key: String, minScore: Double, maxScore: Double): Long {
        return cluster.zcount(key, minScore, maxScore)
    }

    override fun zscore(key: String, member: String): Double? {
        return cluster.zscore(key, member)
    }

    override fun zincrby(key: String, increment: Double, member: String): Double {
        return cluster.zincrby(key, increment, member)
    }

    override fun zrangeByScore(key: String, minScore: Double, maxScore: Double): List<String> {
        return cluster.zrangeByScore(key, minScore, maxScore)
    }

    override fun zrem(key: String, vararg members: String): Long {
        return cluster.zrem(key, *members)
    }

    override fun zremrangeByScore(key: String, minScore: Double, maxScore: Double): Long {
        return cluster.zremrangeByScore(key, minScore, maxScore)
    }

    override fun expire(key: String, seconds: Long): Long {
        return cluster.expire(key, seconds)
    }

    override fun exists(key: String): Boolean {
        return cluster.exists(key)
    }

    override fun set(
        key: String,
        value: String
    ) {
        cluster.set(key, value)
    }

    override fun remove(key: String) {
        cluster.del(key)
    }
}
