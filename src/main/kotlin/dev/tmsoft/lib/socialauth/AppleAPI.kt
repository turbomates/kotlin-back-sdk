package dev.tmsoft.lib.socialauth

import kotlinx.serialization.Serializable

class AppleAPI {
    suspend fun getUser(accessToken: String): AppleUser {
        throw NotImplementedError("$accessToken not used")
    }
}

@Serializable
data class AppleUser(val id: String, val email: String?)
