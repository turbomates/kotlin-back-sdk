package dev.tmsoft.lib.date

import io.ktor.features.DataConversion
import io.ktor.util.DataConversionException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeParseException

fun DataConversion.Configuration.localDateTime() {
    convert<LocalDateTime> {
        decode { values, _ ->
            values.singleOrNull()?.let {
                try {
                    LocalDateTime.parse(it, localDateTimeFormat)
                } catch (ex: DateTimeParseException) {
                    LocalDateTime.of(LocalDate.parse(it, localDateFormat), LocalTime.MIN)
                }
            }
        }
        encode { value ->
            when (value) {
                null -> listOf()
                is LocalDate -> listOf(value.format(localDateFormat))
                else -> throw DataConversionException("Cannot convert $value as LocalDateTime")
            }
        }
    }
}
