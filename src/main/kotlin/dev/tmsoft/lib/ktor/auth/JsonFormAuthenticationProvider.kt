package dev.tmsoft.lib.ktor.auth

import dev.tmsoft.lib.ktor.Response
import dev.tmsoft.lib.validation.Error
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond

class JsonFormAuthenticationProvider<T : Principal> internal constructor(config: Configuration<T>) :
    AuthenticationProvider(config) {

    class Configuration<T : Principal> internal constructor(name: String?) :
        Config(name) {
        var transformer: (Principal, ApplicationCall) -> Principal = { principal, _ -> principal }
        var loginParamName: String = "login"
        var passwordParamName: String = "password"
        lateinit var provider: PrincipalProvider<T>
    }

    /**
     * POST parameter to fetch for a client name
     */
    private val loginParamName: String = config.loginParamName

    /**
     * POST parameter to fetch for a client password
     */
    private val passwordParamName: String = config.passwordParamName

    private val provider: PrincipalProvider<T> = config.provider

    val transformer: (Principal, ApplicationCall) -> Principal = config.transformer

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val postParameters = runCatching{ call.receiveNullable<Map<String, String>>() }.getOrNull()
        val login = postParameters?.get(loginParamName)
        val password = postParameters?.get(passwordParamName)
        val credentials = if (login != null && password != null) UserPasswordCredential(login, password) else null

        val principal = credentials?.let { provider.load(it, call) }
        if (principal != null) {
            context.principal(transformer(principal, call))
            return
        }
        val cause = if (credentials != null) AuthenticationFailedCause.InvalidCredentials else AuthenticationFailedCause.NoCredentials
        @Suppress("NAME_SHADOWING")
        context.challenge(FORM_AUTHENTICATION_CHALLENGE_KEY, cause) { challenge, call ->
            call.respond(
                HttpStatusCode.UnprocessableEntity,
                Response.Errors(listOf(Error(BAD_CREDENTIALS)))
            )
            if (!challenge.completed && call.response.status() != null) {
                challenge.complete()
            }
        }
    }
}

/**
 * Installs Form Authentication mechanism
 */
fun <T : Principal> AuthenticationConfig.jsonForm(
    name: String? = null,
    configure: JsonFormAuthenticationProvider.Configuration<T>.() -> Unit
) {
    val provider =
        JsonFormAuthenticationProvider(JsonFormAuthenticationProvider.Configuration<T>(name).apply(configure))
    register(provider)
}

private const val FORM_AUTHENTICATION_CHALLENGE_KEY: String = "JsonFormAuth"
const val BAD_CREDENTIALS = "authorization.bad.credentials"
