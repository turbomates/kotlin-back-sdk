package dev.tmsoft.lib.email

data class SMTPConfig(
    val host: String,
    val port: Int,
    val smtpSecure: SMTPSecure? = null,
    val username: String? = null,
    val password: String? = null
)

enum class SMTPSecure {
    SSL,
    TLS;
}
