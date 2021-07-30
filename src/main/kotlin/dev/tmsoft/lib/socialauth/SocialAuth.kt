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
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.http.ContentType
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.application
import io.ktor.routing.get
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.serialization.json.Json

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

fun Route.authenticateBySocial(
    configuration: String,
    transformer: SocialPrincipalTransformer,
    successUri: String
): Route {
    val configurationNames = listOf(configuration)
    val authenticatedRoute = createChild(AuthenticationRouteSelector(configurationNames))

    application.feature(Authentication).interceptPipeline(authenticatedRoute, configurationNames)
    transformSocial(authenticatedRoute, transformer, successUri)
    authenticatedRoute {
        get {}
    }

    return authenticatedRoute
}

fun transformSocial(
    pipeline: ApplicationCallPipeline,
    transformer: SocialPrincipalTransformer,
    successUri: String
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
                call.respondRedirect(successUri)
                this.finish()
            } else {
                context.authentication.error("SocialAuth", AuthenticationFailedCause.InvalidCredentials)
            }
        }
    }
}

interface SocialPrincipalTransformer {
    suspend fun transform(principal: OAuthAccessTokenResponse): Principal?
}

internal val socialClient = HttpClient(CIO) {
    install(JsonFeature) {
        accept(ContentType.Application.Json)
        serializer = KotlinxSerializer(
            Json {
                isLenient = true
                ignoreUnknownKeys = true
            }
        )
    }
}
