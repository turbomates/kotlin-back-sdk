package dev.tmsoft.lib.date

import io.ktor.util.converters.DataConversion
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

fun DataConversion.Configuration.localDate() {
    convert<LocalDate> {
        decode { values ->
            values.singleOrNull()?.let {
                try {
                    LocalDate.parse(it, dateFormat)
                } catch (ex: DateTimeParseException) {
                    OffsetDateTime.parse(it, dateTimeFormat).toLocalDate()
                }
            }
            LocalDate.parse(values.singleOrNull())
        }
        encode { value ->
            listOf(value.format(dateFormat))
        }
    }
}
