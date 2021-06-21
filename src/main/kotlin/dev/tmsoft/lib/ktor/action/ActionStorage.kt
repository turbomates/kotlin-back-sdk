package dev.tmsoft.lib.ktor.action

import io.ktor.application.ApplicationCall
import io.ktor.util.pipeline.PipelineContext

interface ActionStorage {
    suspend fun add(context: PipelineContext<Unit, ApplicationCall>, additionalInformation: String?)
}
