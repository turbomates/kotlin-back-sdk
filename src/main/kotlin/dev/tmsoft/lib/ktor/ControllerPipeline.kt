package dev.tmsoft.lib.ktor

import io.ktor.server.application.ApplicationCall
import io.ktor.util.pipeline.PipelineContext
import kotlin.reflect.KClass

interface ControllerPipeline {
    fun <TController : Controller> get(
        clazz: KClass<TController>,
        context: PipelineContext<*, ApplicationCall>
    ): TController
}
