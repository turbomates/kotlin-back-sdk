package dev.tmsoft.lib.socialauth

import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

const val TWITTER_REQUEST_URL = "https://api.twitter.com/oauth/request_token"
const val TWITTER_AUTHORIZE_URL="https://api.twitter.com/oauth/authorize"
const val TWITTER_TOKEN_URL="https://api.twitter.com/oauth/access_token"

fun <T : Principal> Authentication.Configuration.twitter(
    name: String,
    configure: Configuration<T>.() -> Unit
) {

    val configuration = Configuration<T>(name).apply(configure)
    val oauthProvider = OAuthAuthenticationProvider.Configuration(name)
        .apply {
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
        .build()
    val transformer = TwitterTransformer(TwitterAPI(configuration.clientId, configuration.clientSecret), configuration.provider)
    oauthProvider.oauth1a(transformer)
    register(oauthProvider)
}


class TwitterTransformer<T: Principal>(private val api: TwitterAPI, private val provider: SocialProvider<T>): SocialPrincipalTransformer {
    override suspend fun transform(principal: OAuthAccessTokenResponse): Principal? =
        when (principal) {
            is OAuthAccessTokenResponse.OAuth1a -> {
                val user = api.getUser(principal.token, principal.tokenSecret)
                provider.load(user.email, SocialToken(SocialAuthType.TWITTER, principal))
            }
            else -> null
        }
}
