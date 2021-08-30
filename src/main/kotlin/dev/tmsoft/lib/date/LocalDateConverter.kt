package dev.tmsoft.lib.date

import dev.tmsoft.lib.date.localDateFormat
import dev.tmsoft.lib.date.localDateTimeFormat
import io.ktor.features.DataConversion
import io.ktor.util.DataConversionException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

fun DataConversion.Configuration.localDate() {
    convert<LocalDate> {
        decode { values, _ ->
            values.singleOrNull()?.let {
                try {
                    LocalDate.parse(it, localDateFormat)
                } catch (ex: DateTimeParseException) {
                    LocalDateTime.parse(it, localDateTimeFormat).toLocalDate()
                }
            }
            values.singleOrNull()?.let { LocalDate.parse(it) }
        }
        encode { value ->
            when (value) {
                null -> listOf()
                is LocalDate -> listOf(value.format(localDateFormat))
                else -> throw DataConversionException("Cannot convert $value as LocalDate")
            }
        }
    }
}
