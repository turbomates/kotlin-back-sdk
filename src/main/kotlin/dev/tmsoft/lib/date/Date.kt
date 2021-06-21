package dev.tmsoft.lib.date

import java.time.format.DateTimeFormatter

val localDateTimeFormat: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
val localDateFormat: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("yyyy-MM-dd")
