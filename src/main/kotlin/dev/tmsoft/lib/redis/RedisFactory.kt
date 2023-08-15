package dev.tmsoft.lib.redis

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisFactory(private val pool: JedisPool) {
    fun createPersistentMap(): RedisPersistentMap {
        return RedisPersistentMap(pool)
    }

    fun createPersistentHash(): RedisPersistentHash {
        return RedisPersistentHash(pool)
    }

    fun createPersistentList(): RedisPersistentList {
        return RedisPersistentList(pool)
    }

    companion object {
        fun create(
            host: String,
            port: Int,
            maxIdle: Int,
            maxTotal: Int,
            username: String? = null,
            password: String? = null
        ): RedisFactory {
            val jedisPoolConfig = JedisPoolConfig()
            jedisPoolConfig.maxIdle = maxIdle
            jedisPoolConfig.maxTotal = maxTotal
            val pool = if (username != null && password != null) {
                JedisPool(jedisPoolConfig, host, port, 15, username, password)
            } else {
                JedisPool(jedisPoolConfig, host, port)
            }
            return RedisFactory(pool)
        }
    }
}
