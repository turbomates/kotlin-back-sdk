package dev.tmsoft.lib.email

interface Mail {
    suspend fun send(message: Message): Boolean
}
