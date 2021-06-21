package dev.tmsoft.lib.ktor

import kotlin.reflect.KClass

interface InterceptorPipeline {
    fun <TInterceptor : Interceptor> get(clazz: KClass<TInterceptor>): TInterceptor
}
