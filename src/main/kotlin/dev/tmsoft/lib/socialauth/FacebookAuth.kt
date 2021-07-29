package dev.tmsoft.lib.socialauth

import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpMethod

const val FACEBOOK_AUTHORIZE_URL = "https://graph.facebook.com/oauth/authorize"
const val FACEBOOK_TOKEN_URL = "https://graph.facebook.com/oauth/access_token"

fun <T : Principal> Authentication.Configuration.facebook(
    name: String,
    configure: Configuration<T>.() -> Unit
) {
    val configuration = Configuration<T>(name).apply(configure)
    val oauthProvider = OAuthAuthenticationProvider.Configuration(name)
        .apply {
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
        .build()
    val transformer = FacebookTransformer(FacebookAPI(), configuration.provider)
    oauthProvider.oauth2(transformer)
    register(oauthProvider)
}


class FacebookTransformer<T: Principal>(private val api: FacebookAPI, private val provider: SocialProvider<T>): SocialPrincipalTransformer {
    override suspend fun transform(principal: OAuthAccessTokenResponse): Principal? =
        when (principal) {
            is OAuthAccessTokenResponse.OAuth2 -> {
                val user = api.getUser(principal.accessToken)
                provider.load(user.email, SocialToken(SocialAuthType.FACEBOOK, principal))
            }
            else -> null
        }
}
