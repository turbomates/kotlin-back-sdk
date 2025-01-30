package dev.tmsoft.lib.redis

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisFactory(private val pool: JedisPool, private val prefix: String? = null) {
    fun createPersistentMap(): RedisPersistentMap {
        return RedisPersistentMap(pool, prefix)
    }

    fun createPersistentHash(): RedisPersistentHash {
        return RedisPersistentHash(pool, prefix)
    }

    fun createPersistentList(): RedisPersistentList {
        return RedisPersistentList(pool, prefix)
    }

    fun createPersistentSet(): RedisPersistentSet {
        return RedisPersistentSet(pool, prefix)
    }

    companion object {
        @Suppress("LongParameterList")
        fun create(
            host: String,
            port: Int,
            maxIdle: Int,
            maxTotal: Int,
            username: String? = null,
            password: String? = null,
            prefix: String? = null,
            timeout: Int = 10000
        ): RedisFactory {
            val jedisPoolConfig = JedisPoolConfig()
            jedisPoolConfig.maxIdle = maxIdle
            jedisPoolConfig.maxTotal = maxTotal
            val pool = if (username != null && password != null) {
                JedisPool(jedisPoolConfig, host, port, timeout, username, password)
            } else {
                JedisPool(jedisPoolConfig, host, port)
            }
            return RedisFactory(pool, prefix)
        }
    }
}
