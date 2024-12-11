package dev.tmsoft.lib.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.TimeGauge
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.measureTimedValue

suspend fun <T> MeterRegistry.timer(
    name: String,
    tags: Map<String, String> = emptyMap(),
    block: suspend () -> T
): T {
    val timer = Timer.start(this)
    val result =
        try {
            block()
        } finally {
            timer.stop(
                Timer
                    .builder(name).also { builder -> tags.map { builder.tag(it.key, it.value) } }
                    .publishPercentileHistogram()
                    .register(this)
            )
        }
    return result
}

fun MeterRegistry.gauge(
    name: String,
    duration: Duration,
    tags: Map<String, String> = emptyMap()
): TimeGauge =
    TimeGauge
        .builder(name, duration.inWholeMilliseconds, TimeUnit.MILLISECONDS, Long::toDouble)
        .also { builder -> tags.map { builder.tag(it.key, it.value) } }
        .register(this)

suspend fun <T> MeterRegistry.gaugeTimer(
    name: String,
    tags: Map<String, String> = emptyMap(),
    block: suspend () -> T
): T =
    measureTimedValue {
        block()
    }.also {
        gauge(name, it.duration, tags)
    }.value
