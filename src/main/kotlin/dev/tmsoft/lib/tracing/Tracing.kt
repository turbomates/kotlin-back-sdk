package dev.tmsoft.lib.tracing

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext

suspend fun <T> withTrace(
    traceData: TraceData,
    body: suspend () -> T
): T {
    val remoteContext =
        SpanContext.createFromRemoteParent(
            traceData.traceId,
            traceData.spanId,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        )

    val newContext = Context.current().with(Span.wrap(remoteContext))

    return withContext(newContext.asContextElement()) {
        body()
    }
}

suspend fun <T> OpenTelemetry.withNewTrace(
    spanName: String,
    body: suspend () -> T
): T {
    val tracer = getTracer("worker")
    val span = tracer.spanBuilder(spanName).startSpan()

    val newContext = Context.current().with(span)

    return try {
        withContext(newContext.asContextElement()) {
            body()
        }
    } catch (transient: Throwable) {
        span.setStatus(StatusCode.ERROR, transient.message ?: "Error")
        span.recordException(transient)
        throw transient
    } finally {
        span.end()
    }
}

suspend inline fun <T> TraceData?.use(crossinline block: suspend () -> T) =
    if (this == null) {
        block()
    } else {
        withTrace(this) {
            block()
        }
    }
