package dev.tmsoft.lib.ktor.action

import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.feature
import io.ktor.request.ApplicationReceivePipeline
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.request.contentCharset
import io.ktor.response.ApplicationSendPipeline
import io.ktor.routing.Route
import io.ktor.routing.RouteSelector
import io.ktor.routing.RouteSelectorEvaluation
import io.ktor.routing.RoutingResolveContext
import io.ktor.routing.application
import io.ktor.util.AttributeKey
import io.ktor.util.InternalAPI
import io.ktor.util.copyToBoth
import io.ktor.util.pipeline.PipelinePhase
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining

private val actionRecording: AttributeKey<TrackingInformation> = AttributeKey("RouteTracking")

class ActionRecording(config: Configuration) {

    private val storage = config.storage
    private val trackingInformation = config.trackingInformation

    class Configuration {
        lateinit var storage: ActionStorage
        lateinit var trackingInformation: TrackingInformationBuilder
    }

    @OptIn(InternalAPI::class)
    fun interceptPipeline(pipeline: ApplicationCallPipeline) {
        pipeline.receivePipeline.addPhase(AddManagerActionPhase)
        pipeline.receivePipeline.intercept(AddManagerActionPhase) { request ->
            val byteChannel = ByteChannel(false)
            val nextChannel = ByteChannel(false)
            if ((request.value is ByteReadChannel)) {
                val requestValue = request.value as ByteReadChannel
                requestValue.copyToBoth(byteChannel, nextChannel)
            }
            val test = proceedWith(ApplicationReceiveRequest(request.typeInfo, byteChannel, request.reusableValue))
            context.attributes.put(
                actionRecording,
                trackingInformation.build(
                    this,
                    nextChannel.readRemaining().readText(context.request.contentCharset() ?: Charsets.UTF_8),
                    test
                )
            )
        }

        pipeline.sendPipeline.intercept(ApplicationSendPipeline.After) {
            if (context.response.status()?.value == 200) {
                storage.add(context.attributes.getOrNull(actionRecording) ?: trackingInformation.build(this))
            }
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, ActionRecording> {
        val AddManagerActionPhase: PipelinePhase = PipelinePhase("AddManagerAction")
        override val key: AttributeKey<ActionRecording> = AttributeKey("ManagerAction")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): ActionRecording {
            pipeline.receivePipeline.insertPhaseAfter(ApplicationReceivePipeline.Before, AddManagerActionPhase)
            return ActionRecording(Configuration().apply(configure))
        }
    }
}

@OptIn(InternalAPI::class)
@JvmName("AddManagerAction")
fun Route.track(
    build: Route.() -> Unit
): Route {
    val actionRoute = createChild(ActionRouteSelector())
    application.feature(ActionRecording).interceptPipeline(actionRoute)
    actionRoute.build()
    return this
}

class ActionRouteSelector : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Constant

    override fun toString(): String = "(add tracking)"
}
