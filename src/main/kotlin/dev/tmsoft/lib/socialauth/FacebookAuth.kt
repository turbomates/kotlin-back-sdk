package dev.tmsoft.lib.socialauth

import dev.tmsoft.lib.ktor.auth.Principal
import io.ktor.auth.Authentication
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.OAuthServerSettings
import io.ktor.auth.oauth
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpMethod

const val FACEBOOK_AUTHORIZE_URL = "https://graph.facebook.com/oauth/authorize"
const val FACEBOOK_TOKEN_URL = "https://graph.facebook.com/oauth/access_token"

fun Authentication.Configuration.facebook(
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
                authorizeUrl = FACEBOOK_AUTHORIZE_URL,
                accessTokenUrl = FACEBOOK_TOKEN_URL,
                requestMethod = HttpMethod.Post,

                clientId = configuration.clientId,
                clientSecret = configuration.clientSecret,
                defaultScopes = listOf("public_profile", "email")
            )
        }
    }
}

class FacebookTransformer<T : Principal>(private val provider: SocialProvider<T>) : SocialPrincipalTransformer {
    override suspend fun transform(principal: OAuthAccessTokenResponse): Principal? =
        when (principal) {
            is OAuthAccessTokenResponse.OAuth2 -> {
                val api = FacebookAPI()
                val user = api.getUser(principal.accessToken)
                if (user != null) {
                    provider.load(user.email, SocialToken(SocialAuthType.FACEBOOK, principal))
                } else null
            }
            else -> null
        }
}
