package dev.tmsoft.lib.socialauth

import dev.tmsoft.lib.ktor.auth.Principal
import dev.tmsoft.lib.ktor.auth.Session
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationFailedCause
import io.ktor.auth.AuthenticationProvider
import io.ktor.auth.AuthenticationRouteSelector
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.authentication
import io.ktor.routing.Route
import io.ktor.routing.application
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.pipeline.PipelinePhase

class Configuration internal constructor(name: String?) :
    AuthenticationProvider.Configuration(name) {
    lateinit var redirectUrl: String
    lateinit var clientId: String
    lateinit var clientSecret: String
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

fun <T : Principal> Route.authenticateBySocial(
    configuration: String,
    transformer: SocialPrincipalTransformer,
    build: Route.() -> Unit
): Route {
    val configurationNames = listOf(configuration)
    val authenticatedRoute = createChild(AuthenticationRouteSelector(configurationNames))

    application.feature(Authentication).interceptPipeline(authenticatedRoute, configurationNames)
    transformSocial(authenticatedRoute, transformer)
    authenticatedRoute.build()

    return authenticatedRoute
}

fun transformSocial(
    pipeline: ApplicationCallPipeline,
    transformer: SocialPrincipalTransformer
) {
    val phase = PipelinePhase("SocialAuth")
    pipeline.insertPhaseAfter(Authentication.ChallengePhase, phase)
    pipeline.intercept(phase) {
        val oauthPrincipal = call.authentication.principal<OAuthAccessTokenResponse>()
        if (oauthPrincipal != null) {
            val principal = transformer.transform(oauthPrincipal)
            if (principal != null) {
                val session = call.sessions.get<Session>() ?: Session()
                call.sessions.set(session.copy(principal = principal))
            } else {
                context.authentication.error("SocialAuth", AuthenticationFailedCause.InvalidCredentials)
            }
        }
    }
}

interface SocialPrincipalTransformer {
    suspend fun transform(principal: OAuthAccessTokenResponse): Principal?
}
