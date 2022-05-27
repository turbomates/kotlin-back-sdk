package dev.tmsoft.lib.upload

import java.io.ByteArrayInputStream
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class Application(request: String): File() {
    override val extension: String
    private val _content: ByteArrayInputStream
    override val content: ByteArrayInputStream
        get() {
            _content.reset()
            return _content
        }

    init {
        _content = parseContent(request)
        extension = parseExtension(request)
    }

    override fun parseExtension(request: String): String {
        val extensionDelimiter = MimeType.APPLICATION.delimiter
        val startOfType = request.indexOf(extensionDelimiter)
        val endOfType = request.indexOf(DELIMITER)
        if (startOfType == -1) return "json"
        return request
            .substring(startOfType + extensionDelimiter.length, endOfType)
            .normalizeExtension()
    }

    override fun String.normalizeExtension(): String {
        return when {
            contains("gzip") -> "gz"
            contains("vnd.rar") -> "rar"
            contains("x-tar") -> "tar"
            contains("vnd.ms-excel") -> "xls"
            contains("msword") -> "doc"
            contains("wordprocessingml") -> "docx"
            else -> this
        }
    }
}

@Serializer(forClass = Application::class)
object ApplicationSerializer : KSerializer<Application> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Application", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Application) {
        value.content.reset()
        encoder.encodeSerializableValue(ByteArraySerializer(), value.content.readAllBytes())
    }

    override fun deserialize(decoder: Decoder): Application {
        return Application(decoder.decodeString())
    }
}
