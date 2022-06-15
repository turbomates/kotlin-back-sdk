package dev.tmsoft.lib.ktor.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.application.plugin
import io.ktor.server.response.respond
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.server.routing.application
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import org.slf4j.LoggerFactory

class Authorization(private val config: Configuration) {
    constructor() : this(Configuration())

    private val logger = LoggerFactory.getLogger(Authorization::class.java)
    private val rules = RouteAuthorizationRules()

    class Configuration {
        internal var validate: suspend ApplicationCall.(Set<String>) -> Boolean = { false }
        internal var challenge: suspend io.ktor.util.pipeline.PipelineContext<*, ApplicationCall>.() -> Unit =
            { call.respond(HttpStatusCode.Forbidden) }

        fun validate(block: suspend ApplicationCall.(Set<String>) -> Boolean) {
            validate = block
        }

        fun challenge(block: suspend io.ktor.util.pipeline.PipelineContext<*, ApplicationCall>.() -> Unit) {
            challenge = block
        }
    }

    fun rules(): RouteAuthorizationRules {
        return rules
    }

    /**
     * Configures [pipeline] to process authentication by one or multiple auth methods
     */
    fun interceptPipeline(
        pipeline: ApplicationCallPipeline,
        activities: Set<String>,
        block: suspend ApplicationCall.() -> Boolean = { false }
    ) {
        require(activities.isNotEmpty())
        (pipeline as? Route)?.parent?.let {
            rules.addRule(it, activities)
        }
        pipeline.insertPhaseBefore(ApplicationCallPipeline.Call, AuthorizationCheckPhase)
        pipeline.intercept(AuthorizationCheckPhase) {
            if (!config.validate(call, activities) && !call.block()) {
                logger.debug("Unauthorized for activities: " + activities.joinToString(","))
                config.challenge(this)
                finish()
            }
        }
    }

    companion object Plugin : BaseApplicationPlugin<Application, Configuration, Authorization> {
        val AuthorizationCheckPhase: PipelinePhase = PipelinePhase("CheckAuthorize")
        override val key: AttributeKey<Authorization> = AttributeKey("Authorization")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): Authorization {
            return Authorization().apply {
                configure(configure)
            }
        }
    }

    /**
     * Configure already installed feature
     */
    fun configure(block: Configuration.() -> Unit) {
        block(config)
    }
}

fun Route.authorize(
    activities: Set<String>,
    block: suspend ApplicationCall.() -> Boolean = { false },
    build: Route.() -> Unit
): Route {
    val authenticatedRoute = createChild(AuthorizationRouteSelector(activities))
    application.plugin(Authorization).interceptPipeline(authenticatedRoute, activities, block)
    authenticatedRoute.build()
    return authenticatedRoute
}

class AuthorizationRouteSelector(
    internal val activities: Set<String>
) : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
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
    if (selector is AuthorizationRouteSelector) {
        currentActivities.addAll((selector as AuthorizationRouteSelector).activities.map { it })
    }
    return if (children.isEmpty()) {
        val method = (selector as? HttpMethodRouteSelector)?. run { method.value }
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
