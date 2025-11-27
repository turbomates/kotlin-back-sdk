package dev.tmsoft.lib.ktor

import com.google.inject.Inject
import com.google.inject.Injector
import io.ktor.server.routing.RoutingContext
import kotlin.reflect.KClass

class ControllerPipelineFactory @Inject constructor(private val injector: Injector) : ControllerPipeline {
    override fun <TController : Controller> get(
        clazz: KClass<TController>,
        context: RoutingContext
    ): TController {
        val controller = injector.getInstance(clazz.java)
        controller.routingContext = context

        return controller
    }
}
