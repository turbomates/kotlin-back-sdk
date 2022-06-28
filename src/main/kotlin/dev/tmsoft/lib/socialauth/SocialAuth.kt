package dev.tmsoft.lib.socialauth

import dev.tmsoft.lib.ktor.auth.Principal
import dev.tmsoft.lib.ktor.auth.Session
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.json.JsonPlugin
import io.ktor.client.plugins.kotlinx.serializer.KotlinxSerializer
import io.ktor.http.ContentType
import io.ktor.server.routing.Route
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Plugins
import io.ktor.server.application.call
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.AuthenticationRouteSelector
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.authentication
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.serialization.json.Json

class Configuration internal constructor(name: String?) :
    AuthenticationProvider.Config(name) {
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
    // application.plugin(Authentication).interceptPipeline(authenticatedRoute, configurationNames)
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
    pipeline.insertPhaseAfter(Plugins, phase)
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
    install(JsonPlugin) {
        accept(ContentType.Application.Json)
        serializer = KotlinxSerializer(
            Json {
                isLenient = true
                ignoreUnknownKeys = true
            }
        )
    }
}
