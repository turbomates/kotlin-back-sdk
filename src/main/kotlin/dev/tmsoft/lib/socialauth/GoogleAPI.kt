package dev.tmsoft.lib.socialauth

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

private const val USER_URL = "https://www.googleapis.com/oauth2/v3/userinfo"

class GoogleAPI: SocialAPI<GoogleUser> {
    private val logger by lazy { LoggerFactory.getLogger(javaClass) }
    override suspend fun getUser(accessToken: String): GoogleUser? {
        return try {
            socialClient.get("$USER_URL?access_token=$accessToken").body<GoogleUser>()
        } catch (ignore: ClientRequestException) {
            logger.debug("Google auth request error: ${ignore.message} ${ignore.stackTraceToString()}")
            null
        }
    }
}

@Serializable
data class GoogleUser(
    @SerialName("sub")
    override val id: String,
    val name: String?,
    val email: String?
): SocialUser()
