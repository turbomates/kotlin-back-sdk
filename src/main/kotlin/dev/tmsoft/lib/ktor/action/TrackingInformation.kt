package dev.tmsoft.lib.ktor.action

import java.time.LocalDateTime
import java.util.UUID

data class TrackingInformation(
    val uri: String,
    val actor: UUID,
    val content: String? = null,
    val additionalInformation: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
