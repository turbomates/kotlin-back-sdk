package dev.tmsoft.lib.date

import java.time.format.DateTimeFormatter

val localDateTimeFormat: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
val localDateFormat: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("yyyy-MM-dd")
