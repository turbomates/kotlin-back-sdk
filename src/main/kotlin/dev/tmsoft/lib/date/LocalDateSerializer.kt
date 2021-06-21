package dev.tmsoft.lib.date

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializer(forClass = LocalDate::class)
object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.format(localDateFormat))
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }
}

@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(localDateTimeFormat))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), localDateTimeFormat)
    }
}

@Serializer(forClass = LocalTime::class)
object LocalTimeSerializer : KSerializer<LocalTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.STRING)
    private val df: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeString(value.format(df))
    }

    override fun deserialize(decoder: Decoder): LocalTime {
        return LocalTime.parse(decoder.decodeString(), df)
    }
}

@Serializer(forClass = Locale::class)
object LocaleSerializer : KSerializer<Locale> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Locale", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Locale) {
        encoder.encodeString(value.toString().replace("-", "_"))
    }

    override fun deserialize(decoder: Decoder): Locale {
        return Locale(decoder.decodeString().replace("_", "-"))
    }
}
