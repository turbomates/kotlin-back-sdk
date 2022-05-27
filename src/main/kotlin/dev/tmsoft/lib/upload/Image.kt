package dev.tmsoft.lib.upload

import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class Image(request: String): File() {
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
        val extensionDelimiter = MimeType.IMAGE.delimiter
        val startOfType = request.indexOf(extensionDelimiter)
        val endOfType = request.indexOf(DELIMITER)
        if (startOfType == -1) return request.formatName() ?: "svg"
        return request
            .substring(startOfType + extensionDelimiter.length, endOfType)
            .normalizeExtension()
    }

    override fun String.normalizeExtension() = if (contains("svg+xml")) "svg" else this

    private fun String.formatName(): String? {
        val imageInputStream = ImageIO.createImageInputStream(ByteArrayInputStream(Base64.getDecoder().decode(this)))
        val readers = ImageIO.getImageReaders(imageInputStream)
        var format: String? = null

        if (readers.hasNext()) {
            val reader = readers.next()
            format = reader.formatName
        }
        return format
    }
}


@Serializer(forClass = Image::class)
object ImageSerializer : KSerializer<Image> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Image", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Image) {
        value.content.reset()
        encoder.encodeSerializableValue(ByteArraySerializer(), value.content.readAllBytes())
    }

    override fun deserialize(decoder: Decoder): Image {
        return Image(decoder.decodeString())
    }
}
