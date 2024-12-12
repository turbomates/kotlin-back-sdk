package dev.tmsoft.lib.logger

import java.util.UUID
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

inline fun <reified T : Any> T.logger(): Logger {
    return LoggerFactory.getLogger(T::class.java)
}

fun logger(name: String): Logger {
    return LoggerFactory.getLogger(name)
}
fun generateMDC(client: String, map: Map<String, String> = emptyMap()): MDCContext {
    return MDCContext(
        MDC.getCopyOfContextMap() + mapOf(
            "context" to client,
            "request_id" to UUID.randomUUID().toString()
        ) + map
    )
}
