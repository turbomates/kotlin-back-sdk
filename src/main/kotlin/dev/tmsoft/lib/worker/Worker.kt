package dev.tmsoft.lib.worker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

abstract class Worker(
    private val interval: Long,
    private val initialDelay: Long? = null
) : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    fun start() = launch {
        initialDelay?.let {
            delay(it)
        }

        while (isActive) {
            process()
            delay(interval)
        }
    }

    protected abstract fun process()
}
