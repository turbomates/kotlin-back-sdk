package dev.tmsoft.lib.ktor.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.plugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.response.respond
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.util.AttributeKey
import org.slf4j.LoggerFactory

val AuthorizationInterceptors: RouteScopedPlugin<RouteAuthorizationConfig> = createRouteScopedPlugin(
    " AuthorizationInterceptors",
    ::RouteAuthorizationConfig
) {
    val logger = LoggerFactory.getLogger(RouteAuthorizationConfig::class.java)
    val activities = pluginConfig.activities
    val block = pluginConfig.block
    val pluginConfig = application.plugin(Authorization).config
    on(AuthenticationChecked) { call ->
        if (!pluginConfig.validate(call, activities) && !call.block()) {
            logger.debug("Unauthorized for activities: " + activities.joinToString(","))
            pluginConfig.challenge(call)
            return@on
        }
    }
}

class RouteAuthorizationConfig {
    internal var activities: Set<String> = emptySet()
    internal var block: suspend ApplicationCall.() -> Boolean = { false }
}

class Authorization(internal var config: AuthorizationConfiguration) {
    internal fun configure(block: AuthorizationConfiguration.() -> Unit) {
        val newConfiguration = config
        block(newConfiguration)
        config = newConfiguration
    }

    companion object : BaseApplicationPlugin<Application, AuthorizationConfiguration, Authorization> {
        override val key: AttributeKey<Authorization> = AttributeKey("AuthorizationHolder")

        override fun install(pipeline: Application, configure: AuthorizationConfiguration.() -> Unit): Authorization {
            val config = AuthorizationConfiguration().apply(configure)
            return Authorization(config)
        }
    }

    fun rules(): RouteAuthorizationRules {
        return config.rules()
    }
}

class AuthorizationConfiguration {
    private val rules = RouteAuthorizationRules()
    internal var validate: suspend ApplicationCall.(Set<String>) -> Boolean = { false }
    internal var challenge: suspend ApplicationCall.() -> Unit =
        { respond(HttpStatusCode.Forbidden) }

    fun validate(block: suspend ApplicationCall.(Set<String>) -> Boolean) {
        validate = block
    }

    fun challenge(block: suspend ApplicationCall.() -> Unit) {
        challenge = block
    }

    fun rules(): RouteAuthorizationRules {
        return rules
    }
}


fun Route.authorize(
    activities: Set<String>,
    block: suspend ApplicationCall.() -> Boolean = { false },
    build: Route.() -> Unit
): Route {
    require(activities.isNotEmpty()) { "At least one activity name or null for default need to be provided" }

    val distinctActivities = activities.distinct().toSet()
    val authorizationRoute = createChild(AuthorizationRouteSelector(distinctActivities))
    authorizationRoute.attributes.put(
        AuthorizationActivitiesKey,
        AuthorizationActivities(distinctActivities)
    )
    val allActivities = generateSequence(authorizationRoute) { it.parent }
        .mapNotNull { it.attributes.getOrNull(AuthorizationActivitiesKey) }
        .toList()
        .reversed()

    authorizationRoute.install(AuthorizationInterceptors) {
        this.activities = allActivities.fold(emptySet()) { acc, authorizationActivities ->
            acc + authorizationActivities.activities
        }
        this.block = block
    }
    authorizationRoute.build()
    return authorizationRoute
}

class AuthorizationRouteSelector(
    internal val activities: Set<String>
) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        return RouteSelectorEvaluation.Constant
    }

    override fun toString(): String = "(authorize ${activities.joinToString { it }})"
}

class RouteAuthorizationRules {
    private val list: MutableList<Pair<Route, List<String>>> = mutableListOf()
    internal fun addRule(route: Route, permission: Set<String>) {
        list.add(route to permission.map { it })
    }

    fun buildMap(): Map<String, List<String>> {
        val map = mutableMapOf<String, List<String>>()
        list.forEach { conditions ->
            map += conditions.first.buildPath(emptySet())
        }
        return map
    }
}

private fun Route.buildPath(activities: Set<String>): Map<String, List<String>> {
    val currentActivities = activities.toMutableList()
    currentActivities.addAll(attributes.getOrNull(AuthorizationActivitiesKey)?.activities ?: emptySet())
    return when (this) {
        is RoutingNode -> {
            if (children.isEmpty()) {
                val method = (selector as? HttpMethodRouteSelector)?.run { method.value }
                if (activities.isEmpty()) {
                    emptyMap()
                } else {
                    mapOf(method + ":" + toString().replace(Regex("\\/\\(.*?\\)"), "") to currentActivities)
                }
            } else {
                val result = mutableMapOf<String, List<String>>()
                children.forEach { result.putAll(it.buildPath(currentActivities.toSet())) }
                result
            }
        }

        else -> {
            mutableMapOf()
        }
    }

}

internal class AuthorizationActivities(
    val activities: Set<String>,
)

private val AuthorizationActivitiesKey = AttributeKey<AuthorizationActivities>("AuthorizationActivitiesKey")
