package dev.tmsoft.lib.worker

import dev.tmsoft.lib.logger.generateMDC
import dev.tmsoft.lib.metrics.timer
import dev.tmsoft.lib.tracing.withNewTrace
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.OpenTelemetry
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

abstract class Worker(
    private val interval: Long,
    private val meterRegistry: MeterRegistry,
    private val telemetry: OpenTelemetry,
    private val initialDelay: Long? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CoroutineScope by CoroutineScope(dispatcher) {
    private val logger = LoggerFactory.getLogger(javaClass)
    abstract val name: String

    fun start() = launch {
        initialDelay?.let {
            delay(it)
        }
        while (isActive) {
            logger.debug("worker $name was started")
            withContext(generateMDC(name)) {
                try {
                    telemetry.withNewTrace(name) {
                        meterRegistry.timer("workers_timer", mapOf("worker" to name)) {
                            process()
                        }
                    }
                } catch (ignored: Throwable) {
                    Sentry.captureException(ignored) { scope ->
                        scope.setTag("worker", name)
                    }
                    logger.error(ignored.message, ignored)
                }
            }
            delay(interval)
        }
    }

    protected abstract suspend fun process()
}
