package dev.tmsoft.lib.ktor.auth

import io.ktor.auth.Principal
import io.ktor.auth.UserPasswordCredential

interface PrincipalProvider<T : Principal> {
    suspend fun load(credential: UserPasswordCredential, clientIp: String): T?
    suspend fun refresh(principal: T): T?
}
