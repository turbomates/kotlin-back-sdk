package dev.tmsoft.lib.socialauth

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.http.ContentType
import kotlinx.serialization.Serializable

private const val USER_URL = "https://graph.facebook.com/v11.0/me"

class FacebookAPI {
    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            accept(ContentType.Application.Json)
        }
    }

    suspend fun getUser(accessToken: String): FacebookUser {
        return client.get<FacebookUser>(USER_URL + "?fields=id,name,email&access_token=$accessToken")
    }
}

@Serializable
data class FacebookUser(val id: String, val name: String?, val email: String?)
