package dev.tmsoft.lib.vault

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

private const val LIST_KEY = "keys"
private const val AUTH_HEADER = "X-Vault-Token"

class VaultAPI(domain: String, private val token: String) {
    private val domain: String
    private val logger by lazy { LoggerFactory.getLogger(javaClass) }
    private val vaultClient = HttpClient(CIO) {
        defaultRequest { // this: HttpRequestBuilder ->
            header(AUTH_HEADER, token)
            header("Content-Type", "application/json")
        }

        install(JsonFeature) {
            accept(ContentType.Application.Json)
            serializer = KotlinxSerializer(
                kotlinx.serialization.json.Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    init {
        this.domain = domain.trim('/')
    }

    suspend fun listKeys(namespace: String): List<String> {
        return try {
            val secret = vaultClient.get<SecretListKeys>("$domain/v1/$namespace?list=true")
            secret.data.getOrDefault(LIST_KEY, listOf())
        } catch (ignore: ClientRequestException) {
            logger.debug("Vault request error: ${ignore.message} ${ignore.stackTraceToString()}")
            listOf()
        }
    }


    suspend fun read(namespace: String, key: String): Map<String, String?> {
        return try {
            val secret = vaultClient.get<Secret>("$domain/v1/$namespace/$key")
            secret.data
        } catch (ignore: ClientRequestException) {
            logger.debug("Vault request error: ${ignore.message} ${ignore.stackTraceToString()}")
            mapOf()
        }
    }

    suspend fun write(namespace: String, key: String, data: Map<String, String?>): Boolean {
        return try {
            vaultClient.post<String>("$domain/v1/$namespace/$key") {
                body = data
            }
            true
        } catch (ignore: ClientRequestException) {
            logger.debug("Vault request error: ${ignore.message} ${ignore.stackTraceToString()}")
            false
        }
    }

    suspend fun delete(namespace: String, key: String): Boolean {
        return try {
            vaultClient.delete<String>("$domain/v1/$namespace/$key")
            true
        } catch (ignore: ClientRequestException) {
            logger.debug("Vault request error: ${ignore.message} ${ignore.stackTraceToString()}")
            false
        }
    }
}

@Serializable
data class SecretListKeys(
    val request_id: String,
    val lease_id: String,
    val renewable: Boolean,
    val lease_duration: Int,
    val data: Map<String, List<String>>,
    val auth: SecretAuth?,
    val warnings: List<String>?
)

@Serializable
data class Secret(
    val request_id: String,
    val lease_id: String,
    val renewable: Boolean,
    val lease_duration: Int,
    val data: Map<String, String?>,
    val auth: SecretAuth?,
    val warnings: List<String>?
)

@Serializable
data class SecretAuth(
    val client_token: String,
    val accessor: String,
    val policies: List<String>,
    val metadata: Map<String, String>,
    val lease_duration: Int,
    val renewable: Boolean
)
