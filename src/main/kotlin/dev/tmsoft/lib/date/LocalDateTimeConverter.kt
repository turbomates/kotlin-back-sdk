package dev.tmsoft.lib.date

import io.ktor.util.converters.DataConversion
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeParseException

fun DataConversion.Configuration.localDateTime() {
    convert<LocalDateTime> {
        decode { values ->
            values.singleOrNull().let {
                try {
                    LocalDateTime.parse(it, dateTimeFormat)
                } catch (ex: DateTimeParseException) {
                    LocalDateTime.of(LocalDate.parse(it, dateFormat), LocalTime.MIN)
                }
            }
        }
        encode { value ->
            listOf(value.format(dateTimeFormat))
        }
    }
}
