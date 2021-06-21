package dev.tmsoft.lib.ktor.auth

import io.ktor.auth.jwt.JWTCredential

interface JWTProvider<T : Principal> {
    suspend fun load(credential: JWTCredential): T?
}
