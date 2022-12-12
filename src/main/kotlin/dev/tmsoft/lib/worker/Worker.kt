package dev.tmsoft.lib.worker

import io.sentry.Sentry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

abstract class Worker(
    private val interval: Long,
    private val initialDelay: Long? = null,
    private val processedCountLimit: Int? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CoroutineScope by CoroutineScope(dispatcher) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var processedCount = 0

    fun start() = launch {
        initialDelay?.let {
            delay(it)
        }

        while (isActive) {
            try {
                process()
                processedCount += 1
            } catch (logging: Throwable) {
                Sentry.captureException(logging)
                logger.error("Worker exception: $logging", logging)
            }

            if (processedCountLimit != null && processedCount >= processedCountLimit) break
            delay(interval)
        }
    }

    protected abstract suspend fun process()
}
