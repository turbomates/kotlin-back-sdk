package dev.tmsoft.lib.socialauth

import io.ktor.client.request.get
import kotlinx.serialization.Serializable

private const val USER_URL = "https://www.googleapis.com/oauth2/v3/userinfo"

class GoogleAPI {
    suspend fun getUser(accessToken: String): GoogleUser {
        return socialClient.get<GoogleUser>(USER_URL + "?access_token=$accessToken")
    }
}

@Serializable
data class GoogleUser(val sub: String?, val name: String?, val email: String?)
