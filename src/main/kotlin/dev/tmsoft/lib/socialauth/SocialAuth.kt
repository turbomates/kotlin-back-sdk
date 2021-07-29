package dev.tmsoft.lib.socialauth

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.AuthenticationFailedCause
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.AuthenticationProvider
import io.ktor.auth.OAuth1aException
import io.ktor.auth.OAuth2Exception
import io.ktor.auth.OAuthKey
import io.ktor.auth.Principal
import io.ktor.client.HttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

private val Logger: Logger = LoggerFactory.getLogger("dev.tmsoft.lib.socialsauth")

class Configuration<T : Principal> internal constructor(name: String?) :
    AuthenticationProvider.Configuration(name) {
    lateinit var redirectUrl: String
    lateinit var clientId: String
    lateinit var clientSecret: String
    lateinit var provider: SocialProvider<T>
}

interface SocialProvider<T : Principal> {
    suspend fun load(email: String?, socialToken: SocialToken): T?
}

data class SocialToken(val type: SocialAuthType, val principal: OAuthAccessTokenResponse)

enum class SocialAuthType {
    TWITTER,
    FACEBOOK,
    GOOGLE,
    APPLE
}

interface SocialPrincipalTransformer {
    suspend fun transform(principal: OAuthAccessTokenResponse): Principal?
}

open class OAuthAuthenticationProvider internal constructor(config: Configuration) : AuthenticationProvider(config) {

     val client: HttpClient = config.client
     val providerLookup: ApplicationCall.() -> OAuthServerSettings? = config.providerLookup
     val urlProvider: ApplicationCall.(OAuthServerSettings) -> String = config.urlProvider

    open class Configuration internal constructor(name: String?) : AuthenticationProvider.Configuration(name) {

        lateinit var client: HttpClient
        lateinit var providerLookup: ApplicationCall.() -> OAuthServerSettings?
        lateinit var urlProvider: ApplicationCall.(OAuthServerSettings) -> String

        internal fun build() = OAuthAuthenticationProvider(this)
    }
}

fun OAuthAuthenticationProvider.oauth2(transformer: SocialPrincipalTransformer) {
    pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val provider = call.providerLookup()
        if (provider is OAuthServerSettings.OAuth2ServerSettings) {
            val token = call.oauth2HandleCallback()
            val callbackRedirectUrl = call.urlProvider(provider)
            val cause: AuthenticationFailedCause? = if (token == null) {
                AuthenticationFailedCause.NoCredentials
            } else {
                try {
                    val accessToken = oauth2RequestAccessToken(client, provider, callbackRedirectUrl, token)
                    val principal = transformer.transform(accessToken)
                    if (principal === null) {
                        Logger.trace("Internal user provider is empty")
                        AuthenticationFailedCause.InvalidCredentials
                    } else {
                        context.principal(principal)
                        null
                    }
                } catch (cause: OAuth2Exception.InvalidGrant) {
                    Logger.trace("OAuth invalid grant reported: {}", cause.message)
                    AuthenticationFailedCause.InvalidCredentials
                } catch (cause: Throwable) {
                    Logger.trace("OAuth2 request access token failed", cause)
                    context.error(
                        OAuthKey,
                        AuthenticationFailedCause.Error("Failed to request OAuth2 access token due to $cause")
                    )
                    null
                }
            }

            if (cause != null) {
                context.challenge(OAuthKey, cause) {
                    call.redirectAuthenticateOAuth2(
                        provider,
                        callbackRedirectUrl,
                        state = provider.nonceManager.newNonce(),
                        scopes = provider.defaultScopes,
                        interceptor = provider.authorizeUrlInterceptor
                    )
                    it.complete()
                }
            }
        }
    }
}

fun OAuthAuthenticationProvider.oauth1a(transformer: SocialPrincipalTransformer) {
    pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val provider = call.providerLookup()
        if (provider is OAuthServerSettings.OAuth1aServerSettings) {
            val token = call.oauth1aHandleCallback()
            val cause: AuthenticationFailedCause? = if (token == null) {
                AuthenticationFailedCause.NoCredentials
            } else {
                try {
                    val accessToken = requestOAuth1aAccessToken(client, provider, token)
                    val principal = transformer.transform(accessToken)
                    if (principal === null) {
                        Logger.trace("Internal user provider is empty")
                        AuthenticationFailedCause.InvalidCredentials
                    } else {
                        context.principal(principal)
                        null
                    }
                } catch (cause: OAuth1aException.MissingTokenException) {
                    AuthenticationFailedCause.InvalidCredentials
                } catch (cause: Throwable) {
                    context.error(
                        OAuthKey,
                        AuthenticationFailedCause.Error("OAuth1a failed to get OAuth1 access token" + cause.message)
                    )
                    null
                }
            }

            if (cause != null) {
                context.challenge(OAuthKey, cause) { ch ->
                    try {
                        val t = simpleOAuth1aStep1(client, provider, call.urlProvider(provider))
                        call.redirectAuthenticateOAuth1a(provider, t)
                        ch.complete()
                    } catch (ioe: IOException) {
                        context.error(OAuthKey, AuthenticationFailedCause.Error(ioe.message ?: "IOException"))
                    }
                }
            }
        }
    }
}