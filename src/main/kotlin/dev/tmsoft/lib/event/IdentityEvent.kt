package dev.tmsoft.lib.event

import java.util.UUID

data class IdentityEvent(val id: UUID, val event: Event)
