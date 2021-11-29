package dev.tmsoft.lib.socialauth

import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

private const val USER_URL = "https://graph.facebook.com/v11.0/me"

class FacebookAPI: SocialAPI<FacebookUser> {
    private val logger by lazy { LoggerFactory.getLogger(javaClass) }
    override suspend fun getUser(accessToken: String): FacebookUser? {
        return try {
            socialClient.get<FacebookUser>("$USER_URL?fields=id,name,email&access_token=$accessToken")
        } catch (ignore: ClientRequestException) {
            logger.debug("Facebook auth request error: ${ignore.message} ${ignore.stackTraceToString()}")
            null
        }
    }
}

@Serializable
data class FacebookUser(
    override val id: String,
    val name: String?,
    val email: String?
): SocialUser()
