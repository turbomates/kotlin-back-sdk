package dev.tmsoft.lib.email

interface Mail {
    fun send(message: Message): Boolean
}
