package dev.tmsoft.lib.query.exceptions

class InvalidValue(
    message: String, 
    val value: Any? = null
) : Exception(message)
