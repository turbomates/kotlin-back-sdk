package dev.tmsoft.lib.ktor.action

interface ActionStorage {
    suspend fun add(managerAction: TrackingInformation)
}
