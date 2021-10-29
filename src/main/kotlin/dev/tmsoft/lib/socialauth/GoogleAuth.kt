package dev.tmsoft.lib.socialauth

import dev.tmsoft.lib.ktor.auth.Principal
import io.ktor.auth.Authentication
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.OAuthServerSettings
import io.ktor.auth.oauth
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpMethod

const val GOOGLE_AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/auth"
const val GOOGLE_TOKEN_URL = "https://www.googleapis.com/oauth2/v3/token"

fun Authentication.Configuration.google(
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
                authorizeUrl = GOOGLE_AUTHORIZE_URL,
                accessTokenUrl = GOOGLE_TOKEN_URL,
                requestMethod = HttpMethod.Post,

                clientId = configuration.clientId,
                clientSecret = configuration.clientSecret,
                defaultScopes = listOf("https://www.googleapis.com/auth/plus.login", "email")
            )
        }
    }
}

class GoogleTransformer<T : Principal>(private val provider: SocialProvider<T>) : SocialPrincipalTransformer {
    override suspend fun transform(principal: OAuthAccessTokenResponse): Principal? =
        when (principal) {
            is OAuthAccessTokenResponse.OAuth2 -> {
                val api = GoogleAPI()
                val user = api.getUser(principal.accessToken)
                if (user != null) {
                    provider.load(user.email, SocialToken(SocialAuthType.GOOGLE, principal))
                } else null
            }
            else -> null
        }
}
