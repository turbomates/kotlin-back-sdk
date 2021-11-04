package dev.tmsoft.lib.socialauth

import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val USER_URL = "https://www.googleapis.com/oauth2/v3/userinfo"

class GoogleAPI: SocialAPI<GoogleUser> {
    override suspend fun getUser(accessToken: String): GoogleUser? {
        return try {
            socialClient.get<GoogleUser>("$USER_URL?access_token=$accessToken")
        } catch (ignore: ClientRequestException) {
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
