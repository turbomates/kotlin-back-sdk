package dev.tmsoft.lib.upload

import java.io.ByteArrayInputStream
import java.util.Base64
import javax.activation.UnsupportedDataTypeException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal const val DELIMITER = ";base64,"

abstract class File {
    abstract val extension: String
    abstract val content: ByteArrayInputStream

    protected abstract fun String.normalizeExtension(): String
    protected abstract fun parseExtension(request: String): String
    protected open fun parseContent(request: String): ByteArrayInputStream {
        val startOfContent = request.indexOf(DELIMITER)
        if (startOfContent == -1) {
            return ByteArrayInputStream(Base64.getDecoder().decode(request))
        }
        return ByteArrayInputStream(
            Base64.getDecoder().decode(request.substring(startOfContent + DELIMITER.length, request.length))
        )
    }
}

@Serializer(forClass = File::class)
object FileSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("File", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: File) {
        value.content.reset()
        encoder.encodeSerializableValue(ByteArraySerializer(), value.content.readAllBytes())
    }

    override fun deserialize(decoder: Decoder): File {
        val content = decoder.decodeString()
        return when {
            content.contains(MimeType.IMAGE.delimiter) -> Image(content)
            content.contains(MimeType.APPLICATION.delimiter) -> Application(content)
            content.contains(MimeType.TEXT.delimiter) -> Text(content)
            else -> throw UnsupportedDataTypeException("Unsupported file MIME Type")
        }
    }
}

enum class MimeType(val delimiter: String) {
    APPLICATION("application/"),
    IMAGE("image/"),
    TEXT("text/")
}
