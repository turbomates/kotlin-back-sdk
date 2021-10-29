package dev.tmsoft.lib.socialauth

import kotlinx.serialization.Serializable

interface SocialAPI<T: SocialUser> {
    suspend fun getUser(accessToken: String): T?
}

@Serializable
abstract class SocialUser {
    abstract val id: String
}
