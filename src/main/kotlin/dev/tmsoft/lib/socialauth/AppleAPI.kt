package dev.tmsoft.lib.socialauth

import com.auth0.jwt.JWT
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

private const val VALIDATION_URL = "https://appleid.apple.com/auth/token"

class AppleAPI(private val clientSecret: String, private val clientId: String): SocialAPI<AppleUser> {
    override suspend fun getUser(accessToken: String): AppleUser? {
        return try {
            val validatedUser = socialClient.post<ValidatedUser>(VALIDATION_URL) {
                contentType(ContentType.Application.Json)
                body = UserInformation(
                    clientId,
                    clientSecret,
                    accessToken,
                    "authorization_code"
                )
            }
            val updatedInformation = JWT.decode(validatedUser.idToken).claims
            AppleUser(
                updatedInformation["sub"]!!.asString(),
                updatedInformation["email"]?.asString()
            )
        } catch (ignore: ClientRequestException) {
            null
        }
    }
}

@Serializable
data class ValidatedUser(
    val accessToken: String,
    val idToken: String
)

@Serializable
data class AppleUser(
    val id: String,
    val email: String?
)

@Serializable
data class UserInformation(
    val clientId: String,
    val clientSecret: String,
    val code: String?,
    val grantType: String
)
