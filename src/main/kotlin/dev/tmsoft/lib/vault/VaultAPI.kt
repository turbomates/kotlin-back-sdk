package dev.tmsoft.lib.vault

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
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
private data class SecretListKeys(
    @SerialName("request_id")
    val requestId: String,
    @SerialName("lease_id")
    val leaseId: String,
    val renewable: Boolean,
    @SerialName("lease_duration")
    val leaseDuration: Int,
    val data: Map<String, List<String>>,
    val auth: SecretAuth?,
    val warnings: List<String>?
)

@Serializable
private data class Secret(
    @SerialName("request_id")
    val requestId: String,
    @SerialName("lease_id")
    val leaseId: String,
    val renewable: Boolean,
    @SerialName("lease_duration")
    val leaseDuration: Int,
    val data: Map<String, String?>,
    val auth: SecretAuth?,
    val warnings: List<String>?
)

@Serializable
private data class SecretAuth(
    @SerialName("client_token")
    val clientToken: String,
    val accessor: String,
    val policies: List<String>,
    val metadata: Map<String, String>,
    @SerialName("lease_duration")
    val leaseDuration: Int,
    val renewable: Boolean
)

