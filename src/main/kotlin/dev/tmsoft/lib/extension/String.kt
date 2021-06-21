package dev.tmsoft.lib.extension

import io.ktor.util.hex
import java.util.Locale
import java.util.regex.Pattern
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val nonLetterPattern: Pattern = Pattern.compile("\\W", Pattern.MULTILINE or Pattern.UNICODE_CHARACTER_CLASS)
private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()
private val snakeRegex = "_[a-zA-Z]".toRegex()

fun String.underscore(): String {
    return nonLetterPattern.matcher(this.lowercase()).replaceAll("_").trim('_')
}

// String extensions
fun String.camelToSnakeCase(): String {
    return camelRegex.replace(this) {
        "_${it.value}"
    }.lowercase()
}

fun String.snakeToLowerCamelCase(): String {
    return snakeRegex.replace(this) {
        it.value.replace("_", "")
            .lowercase()
    }
}

fun String.snakeToUpperCamelCase(): String {
    return this.snakeToLowerCamelCase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

fun String.parameterize(separator: String = "_"): String {
    val withReplacedSigns = underscore().replace(Regex("""(\d+?)\+"""), "$1 plus")
    val alphaNumeric = Regex("[^A-Za-z0-9_ ]").replace(withReplacedSigns.lowercase(), " ").trim('_', ' ')
    val underscoredSpaces = Regex("\\s+").replace(alphaNumeric, "_")
    return Regex("[_]+").replace(underscoredSpaces, "_").replace("_", separator)
}

fun String.toSlug() = lowercase()
    .replace("\n", " ")
    .replace("[^a-z\\d\\s]".toRegex(), " ")
    .split(" ")
    .joinToString("-")
    .replace("-+".toRegex(), "-")

fun String.hmacHash(key: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
    val hash = mac.doFinal(this.toByteArray())

    return hex(hash)
}
