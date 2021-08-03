package dev.tmsoft.lib.socialauth

import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import io.ktor.client.features.ClientRequestException

private const val USER_URL = "https://www.googleapis.com/oauth2/v3/userinfo"

class GoogleAPI {
    suspend fun getUser(accessToken: String): GoogleUser? {
        return try {
            socialClient.get<GoogleUser>(USER_URL + "?access_token=$accessToken")
        } catch (exception: ClientRequestException) {
            null
        }
    }
}

@Serializable
data class GoogleUser(val sub: String?, val name: String?, val email: String?)
