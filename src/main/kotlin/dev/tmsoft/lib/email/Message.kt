package dev.tmsoft.lib.email

import java.io.File
import java.util.regex.Pattern

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
        val isValid = Pattern.compile(
            "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@" +
                    "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?" +
                    "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\." +
                    "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?" +
                    "[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|" +
                    "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,})$"
        ).matcher(email).matches()
        if (!isValid) {
            throw InvalidEmailAddress(email)
        }
    }
}


class InvalidEmailAddress(email: String) : Exception("Email: $email is invalid")
