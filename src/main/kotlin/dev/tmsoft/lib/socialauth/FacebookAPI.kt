package dev.tmsoft.lib.socialauth

import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import io.ktor.client.features.ClientRequestException

private const val USER_URL = "https://graph.facebook.com/v11.0/me"

class FacebookAPI {
    suspend fun getUser(accessToken: String): FacebookUser? {
        return try {
            socialClient.get<FacebookUser>(USER_URL + "?fields=id,name,email&access_token=$accessToken")
        } catch (exception: ClientRequestException) {
            null
        }
    }
}

@Serializable
data class FacebookUser(val id: String, val name: String?, val email: String?)
