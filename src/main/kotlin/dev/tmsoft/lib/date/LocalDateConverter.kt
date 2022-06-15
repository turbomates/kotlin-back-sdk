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
            values.singleOrNull().let { LocalDate.parse(it) }
        }
        encode { value ->
            listOf(value.format(dateFormat))
        }
    }
}
