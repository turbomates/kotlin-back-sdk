package dev.tmsoft.lib.socialauth

import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

private const val USER_URL = "https://graph.facebook.com/v11.0/me"

class FacebookAPI: SocialAPI<FacebookUser> {
    override suspend fun getUser(accessToken: String): FacebookUser? {
        return try {
            socialClient.get<FacebookUser>("$USER_URL?fields=id,name,email&access_token=$accessToken")
        } catch (ignore: ClientRequestException) {
            null
        }
    }
}

@Serializable
data class FacebookUser(val id: String, val name: String?, val email: String?)
