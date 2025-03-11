package dev.tmsoft.lib.ktor.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.Principal
import io.ktor.server.auth.UserPasswordCredential

interface PrincipalProvider<T : Principal> {
    suspend fun load(credential: UserPasswordCredential, call: ApplicationCall): T?
    suspend fun refresh(principal: T): T?
}
