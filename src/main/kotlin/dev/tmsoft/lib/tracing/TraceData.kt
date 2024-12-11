package dev.tmsoft.lib.tracing

import io.opentelemetry.api.trace.Span
import kotlinx.serialization.Serializable

@Serializable
class TraceData private constructor(
    val traceId: String,
    val spanId: String
) {
    companion object {
        fun captureCurrent(): TraceData {
            val current = Span.current().spanContext
            return TraceData(current.traceId, current.spanId)
        }
    }
}
