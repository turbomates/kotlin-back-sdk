package dev.tmsoft.lib.socialauth

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.http.ContentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val USER_URL = "https://www.googleapis.com/oauth2/v3/userinfo"

class GoogleAPI {
    val client = HttpClient(CIO) {
        install(JsonFeature) {
            accept(ContentType.Application.Json)
            serializer = KotlinxSerializer(
                Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    suspend fun getUser(accessToken: String): GoogleUser {
        return client.get<GoogleUser>(USER_URL + "?access_token=$accessToken")
    }
}

@Serializable
data class GoogleUser(val sub: String?, val name: String?, val email: String?)
