package dev.tmsoft.lib.socialauth

import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpMethod

const val GOOGLE_AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/auth"
const val GOOGLE_TOKEN_URL = "https://www.googleapis.com/oauth2/v3/token"

fun <T : Principal> Authentication.Configuration.google(
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
                    authorizeUrl = GOOGLE_AUTHORIZE_URL,
                    accessTokenUrl = GOOGLE_TOKEN_URL,
                    requestMethod = HttpMethod.Post,

                    clientId = configuration.clientId,
                    clientSecret = configuration.clientSecret,
                    defaultScopes = listOf("https://www.googleapis.com/auth/plus.login", "email")
                )
            }
        }
        .build()
    val transformer = GoogleTransformer(GoogleAPI(), configuration.provider)
    oauthProvider.oauth2(transformer)
    register(oauthProvider)
}


class GoogleTransformer<T: Principal>(private val api: GoogleAPI, private val provider: SocialProvider<T>): SocialPrincipalTransformer {
    override suspend fun transform(principal: OAuthAccessTokenResponse): Principal? =
        when (principal) {
            is OAuthAccessTokenResponse.OAuth2 -> {
                val user = api.getUser(principal.accessToken)
                provider.load(user.email, SocialToken(SocialAuthType.GOOGLE, principal))
            }
            else -> null
        }
}
