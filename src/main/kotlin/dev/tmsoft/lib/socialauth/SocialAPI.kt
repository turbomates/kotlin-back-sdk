package dev.tmsoft.lib.socialauth

interface SocialAPI {
    suspend fun getUser(accessToken: String): SocialUser?
}

abstract class SocialUser

