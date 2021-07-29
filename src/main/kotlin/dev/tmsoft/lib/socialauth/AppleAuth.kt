package dev.tmsoft.lib.socialauth

import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpMethod

const val APPLE_AUTHORIZE_URL = "https://appleid.apple.com/auth/authorize?response_mode=form_post"
const val APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token"

fun <T : Principal> Authentication.Configuration.apple(
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
                    authorizeUrl = APPLE_AUTHORIZE_URL,
                    accessTokenUrl = APPLE_TOKEN_URL,
                    requestMethod = HttpMethod.Post,

                    clientId = configuration.clientId,
                    clientSecret = configuration.clientSecret,
                    defaultScopes = listOf("name", "email")
                )
            }
        }
        .build()
    val transformer = AppleTransformer(AppleAPI(), configuration.provider)
    oauthProvider.oauth2(transformer)
    register(oauthProvider)
}


class AppleTransformer<T: Principal>(private val api: AppleAPI, private val provider: SocialProvider<T>): SocialPrincipalTransformer {
    override suspend fun transform(principal: OAuthAccessTokenResponse): Principal? =
        when (principal) {
            is OAuthAccessTokenResponse.OAuth2 -> {
                val user = api.getUser(principal.accessToken)
                provider.load(user.email, SocialToken(SocialAuthType.APPLE, principal))
            }
            else -> null
        }
}
