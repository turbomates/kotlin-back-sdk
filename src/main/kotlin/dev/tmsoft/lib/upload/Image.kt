package dev.tmsoft.lib.upload

import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class Image(request: String) {
    private val _content: ByteArrayInputStream
    val extension: String
    val content: ByteArrayInputStream
        get() {
            _content.reset()
            return _content
        }

    init {
        _content = parseContent(request)
        extension = parseExtension(request)
    }

    private fun parseContent(request: String): ByteArrayInputStream {
        val startOfContent = request.indexOf(";base64,")
        if (startOfContent == -1) {
            return ByteArrayInputStream(Base64.getDecoder().decode(request))
        }
        return ByteArrayInputStream(Base64.getDecoder().decode(request.substring(startOfContent + 8, request.length)))
    }

    private fun parseExtension(request: String): String {
        val startOfType = request.indexOf("image/")
        val endOfType = request.indexOf(";base64,")
        if (startOfType == -1) {
            val imageInputStream =
                ImageIO.createImageInputStream(ByteArrayInputStream(Base64.getDecoder().decode(request)))
            return imageInputStream.formatName() ?: "svg"
        }
        return request.substring(startOfType + 6, endOfType).normalize()
    }
}

// Mime Type for svg image/svg+xml remove xml
private fun String.normalize(): String {
    return if (contains("svg+xml")) {
        "svg"
    } else {
        this
    }
}

private fun ImageInputStream.formatName(): String? {
    val readers = ImageIO.getImageReaders(this)
    var format: String? = null

    if (readers.hasNext()) {
        val reader = readers.next()
        format = reader.formatName
    }

    return format
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
