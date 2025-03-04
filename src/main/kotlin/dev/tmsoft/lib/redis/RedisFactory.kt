package dev.tmsoft.lib.redis

import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisFactory(private val access: Access, private val prefix: String? = null) {
    fun createPersistentMap(): RedisPersistentMap {
        return RedisPersistentMap(access, prefix)
    }

    fun createPersistentHash(): RedisPersistentHash {
        return RedisPersistentHash(access, prefix)
    }

    fun createPersistentList(): RedisPersistentList {
        return RedisPersistentList(access, prefix)
    }

    fun createPersistentSet(): RedisPersistentSet {
        return RedisPersistentSet(pool, prefix)
    }

    companion object {
        @Suppress("LongParameterList")
        fun create(config: RedisConfig): RedisFactory {
            val access =
                if (config.cluster) {
                    val jc = JedisCluster(
                        setOf(HostAndPort(config.host, config.port)),
                        config.username,
                        config.password
                    )
                    ClusterAccess(jc)
                } else {
                    val jedisPoolConfig = JedisPoolConfig()
                    jedisPoolConfig.maxIdle = config.idle
                    jedisPoolConfig.maxTotal = config.total
                    PoolAccess(
                        JedisPool(
                            jedisPoolConfig,
                            config.host,
                            config.port,
                            config.timeout,
                            config.username,
                            config.password
                        )
                    )
                }
            return RedisFactory(access, config.prefix)
        }
    }
}

data class RedisConfig(
    val host: String,
    val port: Int = 6379,
    val idle: Int = 8,
    val total: Int = 8,
    val username: String? = null,
    val password: String? = null,
    val cluster: Boolean = false,
    val prefix: String? = null,
    val timeout: Int = 10000
)
