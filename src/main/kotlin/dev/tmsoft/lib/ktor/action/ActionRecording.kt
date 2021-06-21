package dev.tmsoft.lib.ktor.action

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.routing.Route
import io.ktor.routing.RouteSelector
import io.ktor.routing.RouteSelectorEvaluation
import io.ktor.routing.RoutingResolveContext
import io.ktor.routing.application
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase

class ActionRecording(config: Configuration) {
    private val storage = config.storage
    private val additionalInformation = config.additionalInformation

    class Configuration {
        lateinit var storage: ActionStorage
        var additionalInformation: AdditionalInformation? = null
    }

    fun interceptPipeline(
        pipeline: ApplicationCallPipeline,
        block: suspend ApplicationCall.() -> Boolean,

    ) {
        pipeline.addPhase(AddManagerActionPhase)
        pipeline.intercept(AddManagerActionPhase) {
            if (!call.block()) {
                storage.add(this, additionalInformation?.get(this))
            }
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, ActionRecording> {
        val AddManagerActionPhase: PipelinePhase = PipelinePhase("AddManagerAction")
        override val key: AttributeKey<ActionRecording> = AttributeKey("ManagerAction")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): ActionRecording {
            pipeline.insertPhaseAfter(ApplicationCallPipeline.Call, AddManagerActionPhase)
            return ActionRecording(Configuration().apply(configure))
        }
    }
}

@JvmName("AddManagerAction")
fun Route.track(
    block: suspend ApplicationCall.() -> Boolean = { false },
    build: Route.() -> Unit
): Route {
    val actionRoute = createChild(ActionRouteSelector())
    application.feature(ActionRecording).interceptPipeline(actionRoute, block)
    actionRoute.build()
    return this
}

class ActionRouteSelector : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Constant

    override fun toString(): String = "(add track)"
}
