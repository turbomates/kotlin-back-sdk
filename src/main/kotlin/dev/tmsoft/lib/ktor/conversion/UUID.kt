package dev.tmsoft.lib.ktor.conversion

import io.ktor.util.converters.DataConversion
import java.util.UUID

fun DataConversion.Configuration.uuid() {
    convert<UUID> {
        decode { values ->
            UUID.fromString(values.single())
        }
        encode { value ->
            listOf(value.toString())
        }
    }
}
