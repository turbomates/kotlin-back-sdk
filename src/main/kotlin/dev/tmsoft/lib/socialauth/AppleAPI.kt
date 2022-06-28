package dev.tmsoft.lib.socialauth

import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.security.interfaces.RSAPublicKey
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

private const val VALIDATION_TOKEN = "https://appleid.apple.com/auth/keys"

class AppleAPI : SocialAPI<AppleUser> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getUser(accessToken: String): AppleUser? {
        return try {
            val token = JWT.decode(accessToken)
            val jwk = UrlJwkProvider(VALIDATION_TOKEN)[token.keyId]
            JWT.require(Algorithm.RSA256(jwk.publicKey as RSAPublicKey,null))
                .build()
                .verify(accessToken)

            if (token.claims.getValue("iss").asString() != "https://appleid.apple.com" ||
                token.claims.getValue("exp").asLong() < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            ) throw JWTVerificationException("Iss didn't come from Apple or time is expired")

            AppleUser(
                token.claims.getValue("sub").asString(),
                token.claims.getValue("email").asString()
            )
        } catch (ignore: JWTVerificationException) {
            logger.debug("Apple `idToken` Verification Exception: ${ignore.message}"); null
        } catch (ignore: AppleTokenValidationException) {
            logger.debug("Apple Token Data Validation Exception: ${ignore.message}"); null
        }
    }
}
class AppleTokenValidationException(message: String) : Exception(message)

@Serializable
data class AppleUser(
    override val id: String,
    val email: String?
): SocialUser()
