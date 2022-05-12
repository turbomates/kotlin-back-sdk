package dev.tmsoft.lib.worker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

abstract class Worker(
    private val interval: Long,
    private val initialDelay: Long? = null
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun start() = launch {
        initialDelay?.let {
            delay(it)
        }

        while (isActive) {
            try {
                process()
            }  catch (logging: Throwable) {
                logger.error("Worker exception: $logging")
            }

            delay(interval)
        }
    }

    protected abstract fun process()
}
