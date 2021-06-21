package dev.tmsoft.lib.ktor

import io.ktor.routing.Route

abstract class Interceptor {
    abstract fun intercept(route: Route)
}
