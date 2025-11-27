package dev.tmsoft.lib.ktor

import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.RoutingContext

abstract class Controller {
    lateinit var routingContext: RoutingContext
    val call: ApplicationCall get() = routingContext.call
}
