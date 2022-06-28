package dev.tmsoft.lib.ktor.auth

import io.ktor.server.auth.jwt.JWTCredential

interface JWTProvider<T : Principal> {
    suspend fun load(credential: JWTCredential): T?
}
