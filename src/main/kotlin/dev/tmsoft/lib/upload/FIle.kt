package dev.tmsoft.lib.upload

import aws.smithy.kotlin.runtime.content.ByteStream
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.activation.UnsupportedDataTypeException
import javax.imageio.ImageIO
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal const val DELIMITER = ";base64,"

class File(
    request: String,
    val mimeType: MimeType
) {
    val contentType: String
    val extension: String
    private val _content: ByteArrayInputStream
    val content: ByteArrayInputStream
        get() {
            _content.reset()
            return _content
        }
    val byteStream: ByteStream
        get() = ByteStream.fromBytes(content.readAllBytes())

    init {
        _content = parseContent(request)
        extension = parseExtension(request)
        contentType = "${mimeType.delimiter}$extension"
    }

    private fun parseContent(request: String): ByteArrayInputStream {
        val startOfContent = request.indexOf(DELIMITER)
        if (startOfContent == -1) {
            return ByteArrayInputStream(Base64.getDecoder().decode(request))
        }
        return ByteArrayInputStream(
            Base64.getDecoder().decode(request.substring(startOfContent + DELIMITER.length, request.length))
        )
    }

    private fun parseExtension(request: String): String {
        val startOfType = request.indexOf(mimeType.delimiter)
        val endOfType = request.indexOf(DELIMITER)
        if (startOfType == -1) {
            return when (mimeType) {
                MimeType.IMAGE -> request.imageFormatName() ?: "svg"
                MimeType.APPLICATION -> "json"
                MimeType.TEXT -> "txt"
            }
        }
        return request
            .substring(startOfType + mimeType.delimiter.length, endOfType)
            .normalizeExtension()
    }

    fun String.normalizeExtension(): String {
        return when {
            contains("svg+xml") -> "svg"
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
            content.contains(MimeType.IMAGE.delimiter) -> File(content, MimeType.IMAGE)
            content.contains(MimeType.APPLICATION.delimiter) -> File(content, MimeType.APPLICATION)
            content.contains(MimeType.TEXT.delimiter) -> File(content, MimeType.TEXT)
            else -> throw UnsupportedDataTypeException("Unsupported file MIME Type")
        }
    }
}

enum class MimeType(val delimiter: String) {
    APPLICATION("application/"),
    IMAGE("image/"),
    TEXT("text/")
}

private fun String.imageFormatName(): String? {
    val imageInputStream = ImageIO.createImageInputStream(ByteArrayInputStream(Base64.getDecoder().decode(this)))
    val readers = ImageIO.getImageReaders(imageInputStream)
    var format: String? = null

    if (readers.hasNext()) {
        val reader = readers.next()
        format = reader.formatName
    }
    return format
}
