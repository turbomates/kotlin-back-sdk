package dev.tmsoft.lib.socialauth

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.util.Base64
import java.util.SortedMap
import java.util.TreeMap
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.microseconds

private const val USER_URL = "https://api.twitter.com/1.1/account/verify_credentials.json"

class TwitterAPI(private val clientKey: String, private val clientSecret: String) {
    private val logger by lazy { LoggerFactory.getLogger(javaClass) }

    suspend fun getUser(accessToken: String, tokenSecret: String): TwitterUser? {
        val oauthTimestamp = System.currentTimeMillis().microseconds.inWholeSeconds.toString()
        val oauthNonce = UUID.randomUUID().toString().lowercase()

        val parameters: SortedMap<String, String> = TreeMap()

        parameters["oauth_consumer_key"] = clientKey
        parameters["oauth_token"] = accessToken
        parameters["oauth_nonce"] = oauthNonce
        parameters["oauth_timestamp"] = oauthTimestamp
        parameters["oauth_signature_method"] = "HMAC-SHA1"
        parameters["oauth_version"] = "1.0"

        val auth = parameters.toMutableMap()
        parameters["include_email"] = "true"

        val parametersString =
            parameters.map { encode(it.key) + "=" + encode(it.value) }.toList()
                .joinToString("&")

        val method = "GET"
        val signatureBaseString = method.uppercase() + "&" + encode(USER_URL) + "&" + encode(parametersString)

        val signingKey = encode(clientSecret) + "&" + encode(tokenSecret)

        val algorithm = "HmacSHA1"
        val keySpec = SecretKeySpec(signingKey.toByteArray(), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(keySpec)

        val signature = Base64
            .getEncoder()
            .encodeToString(mac.doFinal(signatureBaseString.toByteArray()))

        auth["oauth_signature"] = signature
        val authString = auth.map {
            encode(it.key.lowercase()) + "=\"" + encode(it.value) + "\""
        }.toList().joinToString(", ")

        return try {
            socialClient.get("$USER_URL?include_email=true") {
                headers {
                    append(HttpHeaders.Authorization, "OAuth $authString")
                }
            }.body<TwitterUser>()
        } catch (ignore: ClientRequestException) {
            logger.debug("Twitter auth request error: ${ignore.message} ${ignore.stackTraceToString()}")
            null
        }
    }

    private fun encode(s: String): String {
        return URLEncoder.encode(s, "UTF-8")
    }
}

@Serializable
data class TwitterUser(val id: String?, val name: String?, val email: String?)
