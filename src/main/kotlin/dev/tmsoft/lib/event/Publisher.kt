package dev.tmsoft.lib.event

interface Publisher {
    suspend fun publish(event: Event): Boolean
}
