package dev.tmsoft.lib.ktor

import io.ktor.server.routing.Route

abstract class Interceptor {
    abstract fun intercept(route: Route)
}
