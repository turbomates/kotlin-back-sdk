package dev.tmsoft.lib.validation

data class Error(
    val message: String,
    val fallback: String? = null,
    val default: String? = null,
    val property: String? = null,
    val value: Any? = null,
    val parameters: Map<String, *>? = null
)
