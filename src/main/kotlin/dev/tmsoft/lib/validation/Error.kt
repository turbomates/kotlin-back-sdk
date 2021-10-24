package dev.tmsoft.lib.validation

data class Error(val message: String?, val property: String? = null, val value: Any? = null)
