package dev.tmsoft.lib.params.exceptions

class InvalidValue(
    message: String, 
    val value: Any? = null
) : Exception(message)
