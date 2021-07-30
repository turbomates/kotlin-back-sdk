package dev.tmsoft.lib.socialauth

import dev.tmsoft.lib.ktor.auth.Principal
import io.ktor.auth.Authentication
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.OAuthServerSettings
import io.ktor.auth.oauth
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpMethod

const val APPLE_AUTHORIZE_URL = "https://appleid.apple.com/auth/authorize?response_mode=form_post"
const val APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token"

fun Authentication.Configuration.apple(
    name: String,
    configure: Configuration.() -> Unit
) {
    val configuration = Configuration(name).apply(configure)
    this.oauth(name) {
        client = HttpClient(CIO)
        urlProvider = { configuration.redirectUrl }
        providerLookup = {
            OAuthServerSettings.OAuth2ServerSettings(
                name = name,
                authorizeUrl = APPLE_AUTHORIZE_URL,
                accessTokenUrl = APPLE_TOKEN_URL,
                requestMethod = HttpMethod.Post,

                clientId = configuration.clientId,
                clientSecret = configuration.clientSecret,
                defaultScopes = listOf("name", "email")
            )
        }
    }
}

class AppleTransformer<T : Principal>(private val api: AppleAPI, private val provider: SocialProvider<T>) : SocialPrincipalTransformer {
    override suspend fun transform(principal: OAuthAccessTokenResponse): Principal? =
        when (principal) {
            is OAuthAccessTokenResponse.OAuth2 -> {
                val user = api.getUser(principal.accessToken)
                provider.load(user.email, SocialToken(SocialAuthType.APPLE, principal))
            }
            else -> null
        }
}
