package dev.tmsoft.lib.query

interface QueryObject<out T> {
    suspend fun getData(): T
}
