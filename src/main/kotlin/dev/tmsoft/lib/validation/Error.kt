package dev.tmsoft.lib.validation

data class Error(
    val key: String?,
    val namespace: String? = null,
    val property: String? = null,
    val value: Any? = null,
    val parameters: Map<String, *>? = null
)
