package dev.tmsoft.lib.socialauth

import dev.tmsoft.lib.ktor.auth.Principal
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.oauth

const val TWITTER_REQUEST_URL = "https://api.twitter.com/oauth/request_token"
const val TWITTER_AUTHORIZE_URL = "https://api.twitter.com/oauth/authorize"
const val TWITTER_TOKEN_URL = "https://api.twitter.com/oauth/access_token"

fun AuthenticationConfig.twitter(
    name: String,
    configure: Configuration.() -> Unit
) {

    val configuration = Configuration(name).apply(configure)
    this.oauth(name) {
        client = HttpClient(CIO)
        urlProvider = { configuration.redirectUrl }
        providerLookup = {
            OAuthServerSettings.OAuth1aServerSettings(
                name = name,
                authorizeUrl = TWITTER_AUTHORIZE_URL,
                accessTokenUrl = TWITTER_TOKEN_URL,
                requestTokenUrl = TWITTER_REQUEST_URL,

                consumerKey = configuration.clientId,
                consumerSecret = configuration.clientSecret
            )
        }
    }
}

class TwitterTransformer<T : Principal>(private val clientKey: String, private val clientSecret: String, private val provider: SocialProvider<T>) : SocialPrincipalTransformer {
    override suspend fun transform(principal: OAuthAccessTokenResponse): Principal? =
        when (principal) {
            is OAuthAccessTokenResponse.OAuth1a -> {
                val api = TwitterAPI(clientKey, clientSecret)
                val user = api.getUser(principal.token, principal.tokenSecret)
                if (user != null) {
                    provider.load(user.email, SocialToken(SocialAuthType.TWITTER, principal))
                } else null
            }
            else -> null
        }
}
