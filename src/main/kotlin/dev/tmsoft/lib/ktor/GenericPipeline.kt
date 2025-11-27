package dev.tmsoft.lib.ktor

import com.google.inject.Inject
import io.ktor.server.routing.RoutingContext
import kotlin.reflect.KClass

class GenericPipeline @Inject constructor(
    private val controllerPipeline: ControllerPipeline,
    private val interceptorPipeline: InterceptorPipeline
) {
    fun <TController : Controller> controller(
        clazz: KClass<TController>,
        context: RoutingContext
    ): TController {
        return controllerPipeline.get(clazz, context)
    }

    fun <TInterceptor : Interceptor> interceptor(clazz: KClass<TInterceptor>): TInterceptor {
        return interceptorPipeline.get(clazz)
    }
}
