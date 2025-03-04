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
