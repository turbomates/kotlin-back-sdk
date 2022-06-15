package dev.tmsoft.lib.date

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

val dateTimeFormat: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)
val dateFormat: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
