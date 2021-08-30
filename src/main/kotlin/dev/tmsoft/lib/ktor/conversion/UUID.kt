package dev.tmsoft.lib.ktor.conversion

import io.ktor.features.DataConversion
import io.ktor.util.DataConversionException
import java.util.UUID

fun DataConversion.Configuration.uuid() {
    convert<UUID> {
        decode { values, _ ->
            values.singleOrNull()?.let { UUID.fromString(it) }
        }
        encode { value ->
            when (value) {
                null -> listOf()
                is UUID -> listOf(value.toString())
                else -> throw DataConversionException("Cannot convert $value as UUID")
            }
        }
    }
}
