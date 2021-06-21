package dev.tmsoft.lib.validation

import dev.tmsoft.lib.ktor.ErrorSerializer
import kotlinx.serialization.Serializable

@Serializable(with = ErrorSerializer::class)
data class Error(val message: String?, val property: String? = null, val value: Any? = null)
