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

class Text(request: String): File() {
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
        val extensionDelimiter = MimeType.TEXT.delimiter
        val startOfType = request.indexOf(extensionDelimiter)
        val endOfType = request.indexOf(DELIMITER)
        if (startOfType == -1) return "txt"
        return request
            .substring(startOfType + extensionDelimiter.length, endOfType)
            .normalizeExtension()
    }

    override fun String.normalizeExtension(): String = this
}


@Serializer(forClass = Text::class)
object TextSerializer : KSerializer<Text> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Text", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Text) {
        value.content.reset()
        encoder.encodeSerializableValue(ByteArraySerializer(), value.content.readAllBytes())
    }

    override fun deserialize(decoder: Decoder): Text {
        return Text(decoder.decodeString())
    }
}
