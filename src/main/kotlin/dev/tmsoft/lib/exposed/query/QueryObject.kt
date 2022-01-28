package dev.tmsoft.lib.exposed.query

interface QueryObject<out T> {
    suspend fun getData(): T
}
