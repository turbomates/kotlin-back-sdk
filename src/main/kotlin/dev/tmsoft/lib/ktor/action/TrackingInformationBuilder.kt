package dev.tmsoft.lib.ktor.action

import io.ktor.application.ApplicationCall
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.util.pipeline.PipelineContext

interface TrackingInformationBuilder {
    suspend fun build(
        call: PipelineContext<*, ApplicationCall>,
        content: String? = null,
        subject: ApplicationReceiveRequest? = null
    ): TrackingInformation
}
