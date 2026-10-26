package dev.tmsoft.lib.redis

import dev.tmsoft.lib.logger.logger

class RedisPersistentZSet(
    private val access: Access,
    private val prefix: String? = null,
) {
    private val logger = logger()

    fun add(key: String, score: Double, member: String) {
        logger.debug("Add member for key: {}, score: {}, member: {}", key, score, member)
        access.zadd(prefixedKey(key), score, member)
    }

    fun addAll(key: String, members: Map<String, Double>) {
        if (members.isEmpty()) return
        logger.debug("Add members for key: {}, members count: {}", key, members.size)
        access.zadd(prefixedKey(key), members)
    }

    fun count(key: String, minScore: Double, maxScore: Double): Long {
        logger.debug("Count members for key: {}, minScore: {}, maxScore: {}", key, minScore, maxScore)
        return access.zcount(prefixedKey(key), minScore, maxScore)
    }

    fun score(key: String, member: String): Double? {
        logger.debug("Get score for key: {}, member: {}", key, member)
        return access.zscore(prefixedKey(key), member)
    }

    fun incrementScore(key: String, increment: Double, member: String): Double {
        logger.debug("Increment score for key: {}, increment: {}, member: {}", key, increment, member)
        return access.zincrby(prefixedKey(key), increment, member)
    }

    fun rangeByScore(key: String, minScore: Double, maxScore: Double): Set<String> {
        logger.debug("Get range by score for key: {}, minScore: {}, maxScore: {}", key, minScore, maxScore)
        return access.zrangeByScore(prefixedKey(key), minScore, maxScore).toSet()
    }

    @Suppress("SpreadOperator")
    fun remove(key: String, members: List<String>): Long {
        if (members.isEmpty()) return 0
        logger.debug("Remove members for key: {}, members: {}", key, members)
        return access.zrem(prefixedKey(key), *members.toTypedArray())
    }

    fun removeByScore(key: String, minScore: Double, maxScore: Double): Long {
        logger.debug("Remove by score for key: {}, minScore: {}, maxScore: {}", key, minScore, maxScore)
        return access.zremrangeByScore(prefixedKey(key), minScore, maxScore)
    }

    fun exists(key: String): Boolean {
        logger.debug("Check if key exists: {}", key)
        return access.exists(prefixedKey(key))
    }

    fun expire(key: String, seconds: Long): Long {
        logger.debug("Expire key: {}, seconds: {}", key, seconds)
        return access.expire(prefixedKey(key), seconds)
    }

    private fun prefixedKey(key: String): String = prefix?.let { "$prefix:$key" } ?: key
}
