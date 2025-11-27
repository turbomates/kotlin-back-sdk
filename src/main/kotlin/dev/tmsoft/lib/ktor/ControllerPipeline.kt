package dev.tmsoft.lib.ktor

import io.ktor.server.routing.RoutingContext
import kotlin.reflect.KClass

interface ControllerPipeline {
    fun <TController : Controller> get(
        clazz: KClass<TController>,
        context: RoutingContext
    ): TController
}
