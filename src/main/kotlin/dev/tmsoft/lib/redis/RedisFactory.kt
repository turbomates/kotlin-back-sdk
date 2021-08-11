package dev.tmsoft.lib.redis

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisFactory(private val pool: JedisPool) {
    companion object {
        fun create(host: String, port: Int, maxIdle: Int, maxTotal: Int): RedisFactory {
            val jedisPoolConfig = JedisPoolConfig()
            jedisPoolConfig.maxIdle = maxIdle
            jedisPoolConfig.maxTotal = maxTotal

            val pool = JedisPool(jedisPoolConfig, host, port)
            return RedisFactory(pool)
        }
    }

    fun createPersistentMap(): RedisPersistentMap {
        return RedisPersistentMap(pool)
    }

    fun createPersistentHash(): RedisPersistentHash {
        return RedisPersistentHash(pool)
    }

    fun createPersistentList(): RedisPersistentList {
        return RedisPersistentList(pool)
    }
}
