package dev.tmsoft.lib.ktor

import io.ktor.http.Parameters
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

fun Parameters.getLong(name: String): Long {
    val rawValue = this[name] ?: throw BadParameterException(name)
    return rawValue.toLong()
}

fun Parameters.getLongOrNull(name: String): Long? {
    val rawValue = this[name] ?: return null
    return rawValue.toLongOrNull()
}

fun Parameters.getInt(name: String): Int {
    val rawValue = this[name] ?: throw BadParameterException(name)
    return rawValue.toInt()
}

fun Parameters.getIntOrNull(name: String): Int? {
    val rawValue = this[name] ?: return null
    return rawValue.toIntOrNull()
}

fun Parameters.getString(name: String): String {
    return this[name] ?: throw BadParameterException(name)
}

fun Parameters.getStringOrNull(name: String): String? {
    return this[name]
}

fun Parameters.getStrings(name: String): List<String> {
    val rawValue = this[name] ?: return listOf()
    return rawValue.split(",")
}

fun Parameters.getLongs(name: String): List<Long> {
    val rawValue = this[name] ?: return listOf()
    return rawValue.split(",").mapNotNull { it.toLongOrNull() }
        .map { it }
}

inline fun <reified T> Parameters.listOf(name: String): List<T> {
    val rawValue = getStringOrNull(name) ?: return listOf()
    val values = rawValue.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }.split(",").filter { it.isNotBlank() && it.isNotEmpty() }
    val castedValues = mutableListOf<T>()

    when (T::class) {
        String::class -> {
            for (value in values) {
                castedValues.add(value as T)
            }
        }

        Int::class -> {
            for (value in values) {
                value.toIntOrNull()?.let {
                    castedValues.add(it as T)
                }
            }
        }

        Long::class -> {
            for (value in values) {
                value.toLongOrNull()?.let {
                    castedValues.add(it as T)
                }
            }
        }

        else -> {
            throw IllegalArgumentException("Unsupported parameter type: ${T::class.java.name}")
        }
    }

    return castedValues
}

inline fun <reified T> Parameters.setOf(name: String): Set<T> = listOf<T>(name).toSet()

fun Parameters.getBoolean(name: String): Boolean {
    return when (this[name] ?: return false) {
        "1", "true", "TRUE" -> true
        else -> false
    }
}

fun Parameters.getBooleanOrNull(name: String): Boolean? {
    return when (this[name]) {
        "1", "true", "TRUE" -> true
        "0", "false", "FALSE" -> false
        else -> null
    }
}

class BadParameterException(name: String): IllegalArgumentException("Parameter `$name` is not present")
