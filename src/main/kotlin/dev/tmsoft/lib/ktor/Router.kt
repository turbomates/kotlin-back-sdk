package dev.tmsoft.lib.ktor

import com.google.inject.Inject
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.util.pipeline.PipelineContext

open class Router @Inject constructor(
    protected val application: Application,
    protected val genericPipeline: GenericPipeline
) {

    protected inline fun <reified TController : Controller> controller(context: PipelineContext<*, ApplicationCall>): TController {
        return genericPipeline.controller(TController::class, context)
    }

    protected inline fun <reified TInterceptor : Interceptor> intercept(route: Route) {
        return genericPipeline.interceptor(TInterceptor::class).intercept(route)
    }
}
