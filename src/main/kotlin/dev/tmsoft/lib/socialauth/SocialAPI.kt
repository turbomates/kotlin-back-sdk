package dev.tmsoft.lib.socialauth

interface SocialAPI<T> {
    suspend fun getUser(accessToken: String): T?
}
