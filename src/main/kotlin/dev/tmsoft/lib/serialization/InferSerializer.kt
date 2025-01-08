package dev.tmsoft.lib.serialization

import com.turbomates.time.LocalDateSerializer
import com.turbomates.time.LocalDateTimeSerializer
import com.turbomates.time.OffsetDateTimeSerializer
import dev.tmsoft.lib.ktor.Response
import dev.tmsoft.lib.ktor.ResponseDataSerializer
import dev.tmsoft.lib.ktor.ResponseEitherSerializer
import dev.tmsoft.lib.ktor.ResponseErrorSerializer
import dev.tmsoft.lib.ktor.ResponseListingSerializer
import dev.tmsoft.lib.ktor.ResponseOkSerializer
import dev.tmsoft.lib.validation.Error
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.Locale
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.starProjectedType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapEntrySerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

@InternalSerializationApi
fun Collection<*>.elementSerializer(): KSerializer<*> {
    val serializers = mapNotNull { value ->
        value?.let { resolveSerializer(it) }
    }.distinctBy { it.descriptor.serialName }

    if (serializers.size > 1) {
        val supertype = this.first()!!.javaClass.kotlin.supertypes.first()
        @Suppress("UNCHECKED_CAST")
        return serializer(supertype) as KSerializer<Any>
    }

    val selected: KSerializer<*> = serializers.singleOrNull() ?: String::class.serializer()
    if (selected.descriptor.isNullable) {
        return selected
    }

    @Suppress("UNCHECKED_CAST")
    selected as KSerializer<Any>

    if (any { it == null }) {
        return selected.nullable
    }

    return selected
}

@InternalSerializationApi
@Suppress("UNCHECKED_CAST")
fun resolveSerializer(value: Any): KSerializer<Any> {
    return when (value) {
        is JsonElement -> JsonElement::class.serializer()
        is List<*> -> ListSerializer(value.elementSerializer())
        is Set<*> -> SetSerializer(value.elementSerializer())
        is Map<*, *> -> MapSerializer(value.keys.elementSerializer(), value.values.elementSerializer())
        is Map.Entry<*, *> -> mapEntrySerializer(value)
        is Array<*> -> arraySerializer(value)
        is LocalDate -> LocalDateSerializer
        is OffsetDateTime -> OffsetDateTimeSerializer
        is LocalDateTime -> LocalDateTimeSerializer
        is Locale -> LocaleSerializer
        is UUID -> UUIDSerializer
        is Response -> responseSerializer(value)
        is Error -> ResponseErrorSerializer
        else -> value::class.serializer()
    } as KSerializer<Any>
}

private fun mapEntrySerializer(value: Map.Entry<*, *>): KSerializer<out Map.Entry<Any?, Any?>> {
    return MapEntrySerializer(
        resolveSerializer(value.key ?: error("Map.Entry(null, ...) is not supported")),
        resolveSerializer(value.value ?: error("Map.Entry(..., null) is not supported)"))
    )
}

private fun arraySerializer(value: Array<*>): KSerializer<Array<Any>> {
    val componentType = value.javaClass.componentType.kotlin.starProjectedType
    val componentClass = componentType.classifier as? KClass<*> ?: error("Unsupported component type $componentType")
    @Suppress("UNCHECKED_CAST")
    return ArraySerializer(componentClass as KClass<Any>, serializer(componentType) as KSerializer<Any>)
}

private fun responseSerializer(value: Response): KSerializer<*> {
    return when (value) {
        is Response.Ok -> ResponseOkSerializer
        is Response.Listing<*> -> ResponseListingSerializer
        is Response.Either<*, *> -> ResponseEitherSerializer
        is Response.Data<*> -> ResponseDataSerializer
        else -> value::class.serializer()
    }
}

