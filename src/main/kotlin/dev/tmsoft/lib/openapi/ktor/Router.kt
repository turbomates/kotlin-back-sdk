package dev.tmsoft.lib.openapi.ktor

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.http.HttpMethod
import io.ktor.locations.locations
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.application
import io.ktor.routing.method
import io.ktor.routing.route
import io.ktor.util.pipeline.PipelineContext
import kotlin.reflect.typeOf

inline fun <reified TResponse : Any> Route.post(
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.() -> TResponse
): Route {
    return method(HttpMethod.Post) {
        handle {
            call.respond(body())
        }
    }
}

inline fun <reified TResponse : Any> Route.post(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.() -> TResponse
): Route {
    val route = route(path, HttpMethod.Post) {
        handle {
            call.respond(body())
        }
    }
    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Post,
        typeOf<TResponse>()
    )
    return route
}

inline fun <reified TResponse : Any, reified TBody : Any> Route.post(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TBody) -> TResponse
): Route {
    val route = route(path, HttpMethod.Post) {
        handle {
            call.respond(body(call.receive()))
        }
    }
    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Post,
        typeOf<TResponse>(),
        typeOf<TBody>()
    )
    return route
}

inline fun <reified TResponse : Any, reified TParams : Any> Route.postParams(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TParams) -> TResponse
): Route {
    val route = route(path, HttpMethod.Post) {
        handle {
            call.respond(body(locations.resolve(TParams::class, call)))
        }
    }
    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Post,
        typeOf<TResponse>(),
        pathParams = typeOf<TParams>()
    )
    return route
}

inline fun <reified TResponse : Any, reified TBody : Any, reified TParams : Any> Route.post(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TBody, TParams) -> TResponse
): Route {
    val route = route(path, HttpMethod.Post) {
        handle {
            call.respond(body(call.receive(), locations.resolve(TParams::class, call)))
        }
    }

    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Post,
        typeOf<TResponse>(),
        typeOf<TBody>(),
        typeOf<TParams>()
    )
    return route
}

inline fun <reified TResponse : Any, reified TBody : Any, reified TQuery : Any, reified TPath : Any> Route.post(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TBody, TPath, TQuery) -> TResponse
): Route {
    val route = route(path, HttpMethod.Post) {
        handle {
            call.respond(
                body(
                    call.receive(),
                    locations.resolve(TPath::class, call),
                    locations.resolve(TQuery::class, call)
                )
            )
        }
    }

    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Post,
        typeOf<TResponse>(),
        typeOf<TBody>(),
        typeOf<TQuery>()
    )
    return route
}

inline fun <reified TResponse : Any> Route.get(
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.() -> TResponse
): Route {
    return method(HttpMethod.Get) {
        handle {
            call.respond(body())
        }
    }
}

inline fun <reified TResponse : Any> Route.get(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.() -> TResponse
): Route {

    val route = route(path, HttpMethod.Get) {
        handle {
            call.respond(body())
        }
    }
    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Get,
        typeOf<TResponse>()
    )
    return route
}

inline fun <reified TResponse : Any, reified TParams : Any> Route.get(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TParams) -> TResponse
): Route {
    val route = route(path, HttpMethod.Get) {
        handle {
            call.respond(body(locations.resolve(TParams::class, call)))
        }
    }
    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Get,
        typeOf<TResponse>(),
        null,
        typeOf<TParams>()
    )
    return route
}

inline fun <reified TResponse : Any, reified TQuery : Any, reified TPath : Any> Route.get(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TPath, TQuery) -> TResponse
): Route {
    val route = route(path, HttpMethod.Get) {
        handle {
            call.respond(body(locations.resolve(TPath::class, call), locations.resolve(TQuery::class, call)))
        }
    }
    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Get,
        typeOf<TResponse>(),
        typeOf<TQuery>(),
        typeOf<TPath>()
    )
    return route
}

inline fun <reified TResponse : Any> Route.delete(
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.() -> TResponse
): Route {
    return method(HttpMethod.Delete) {
        handle {
            call.respond(body())
        }
    }
}

inline fun <reified TResponse : Any> Route.delete(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.() -> TResponse
): Route {
    return route(path, HttpMethod.Delete) {
        handle {
            call.respond(body())
        }
    }
}

inline fun <reified TResponse : Any, reified TParams : Any> Route.delete(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TParams) -> TResponse
): Route {
    return route(path, HttpMethod.Delete) {
        handle {
            call.respond(body(locations.resolve(TParams::class, call)))
        }
    }
}

inline fun <reified TResponse : Any, reified TBody : Any> Route.deleteWithBody(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TBody) -> TResponse
): Route {
    return route(path, HttpMethod.Delete) {
        handle {
            call.respond(body(call.receive()))
        }
    }
}

inline fun <reified TResponse : Any, reified TQuery : Any, reified TPath : Any> Route.delete(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TPath, TQuery) -> TResponse
): Route {
    return route(path, HttpMethod.Delete) {
        handle {
            call.respond(body(locations.resolve(TPath::class, call), locations.resolve(TQuery::class, call)))
        }
    }
}

inline fun <reified TResponse : Any, reified TQuery : Any, reified TPath : Any, reified TBody : Any> Route.delete(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TPath, TQuery, TBody) -> TResponse
): Route {
    return route(path, HttpMethod.Delete) {
        handle {
            call.respond(
                body(
                    locations.resolve(TPath::class, call),
                    locations.resolve(TQuery::class, call),
                    call.receive()
                )
            )
        }
    }
}

fun Route.buildFullPath(): String {
    return toString().replace(Regex("\\/\\(.*?\\)"), "")
}

val Route.openApi: OpenAPI
    get() {
        return application.feature(OpenAPI)
    }
