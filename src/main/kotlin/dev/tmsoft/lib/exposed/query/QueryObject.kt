package dev.tmsoft.lib.exposed.query

import kotlin.time.Duration.Companion.days

interface QueryObject<out T> {
    suspend fun getData(): T
}

@Suppress("EqualsWithHashCodeExist")
interface CachedQueryObject<out T> : QueryObject<T> {
    companion object {
        val TTL = 14.days
    }

    fun key(): String

    fun ttl() = TTL.inWholeSeconds
}
