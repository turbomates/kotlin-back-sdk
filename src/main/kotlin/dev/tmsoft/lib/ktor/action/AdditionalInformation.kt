package dev.tmsoft.lib.ktor.action

import io.ktor.application.ApplicationCall
import io.ktor.util.pipeline.PipelineContext

interface AdditionalInformation {
    suspend fun get(pipeline: PipelineContext<Unit, ApplicationCall>): String?
}
