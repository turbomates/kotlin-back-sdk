package dev.tmsoft.lib.ktor.auth

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationFailedCause
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.AuthenticationProvider
import io.ktor.auth.UserPasswordCredential
import io.ktor.features.origin
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveOrNull
import io.ktor.response.respond

/**
 * Represents a form-based authentication provider
 * @param name is the name of the provider, or `null` for a default provider
 */
class JsonFormAuthenticationProvider<T : Principal> internal constructor(config: Configuration<T>) :
    AuthenticationProvider(config) {

    class Configuration<T : Principal> internal constructor(name: String?) :
        AuthenticationProvider.Configuration(name) {
        var transformer: (Principal, ApplicationCall) -> Principal = { principal, _ -> principal }
        var loginParamName: String = "login"
        var passwordParamName: String = "password"
        lateinit var provider: PrincipalProvider<T>
    }

    /**
     * POST parameter to fetch for a client name
     */
    val loginParamName: String = config.loginParamName

    /**
     * POST parameter to fetch for a client password
     */
    val passwordParamName: String = config.passwordParamName

    val provider: PrincipalProvider<T> = config.provider

    val transformer: (Principal, ApplicationCall) -> Principal = config.transformer
}

/**
 * Installs Form Authentication mechanism
 */
fun <T : Principal> Authentication.Configuration.jsonForm(
    name: String? = null,
    configure: JsonFormAuthenticationProvider.Configuration<T>.() -> Unit
) {
    val provider =
        JsonFormAuthenticationProvider(JsonFormAuthenticationProvider.Configuration<T>(name).apply(configure))
    val emailParamName = provider.loginParamName
    val passwordParamName = provider.passwordParamName
    val authProvider = provider.provider
    provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val postParameters = call.receiveOrNull<Map<String, String>>()
        val login = postParameters?.get(emailParamName)
        val password = postParameters?.get(passwordParamName)
        val credentials = if (login != null && password != null) UserPasswordCredential(login, password) else null

        val principal = credentials?.let { authProvider.load(it, call.request.origin.remoteHost) }

        if (principal != null) {
            context.principal(provider.transformer(principal, call))
        } else {
            val cause =
                if (credentials == null) AuthenticationFailedCause.NoCredentials else AuthenticationFailedCause.InvalidCredentials
            context.challenge(formAuthenticationChallengeKey, cause) {
                call.respond(HttpStatusCode.Unauthorized, "Bad credentials")
                it.complete()
            }
        }
    }
    register(provider)
}

private val formAuthenticationChallengeKey: Any = "JsonFormAuth"
