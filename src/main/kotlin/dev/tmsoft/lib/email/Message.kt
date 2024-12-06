package dev.tmsoft.lib.email

import java.io.File
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress

class Message(val from: Address, val to: List<Address>, val subject: String?, block: Message.() -> Unit) {
    var replyTo: List<Address> = emptyList()
    var html: String? = null
    var plain: String? = null
    var attaches: List<File> = emptyList()

    init {
        block()
    }
}

data class Address(val email: String, val name: String? = null) {
    init {
        try {
            val address = InternetAddress(email)
            address.validate()
        } catch (transient: AddressException) {
            throw InvalidEmailAddress(email)
        }
    }
}


class InvalidEmailAddress(email: String) : Exception("Email: $email is invalid")
