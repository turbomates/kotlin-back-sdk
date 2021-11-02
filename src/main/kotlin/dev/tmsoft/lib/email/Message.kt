package dev.tmsoft.lib.email

import java.io.File
import java.util.regex.Pattern

class Message(val from: Address, val to: List<Address>, val subject: String?, block: Message.() -> Unit) {
    val replyTo: MutableList<Address> = mutableListOf()
    var html: String? = null
    var plain: String? = null
    val attaches: MutableList<File> = mutableListOf()

    init {
        block()
    }
}

data class Address(val email: Email, val name: String)

@JvmInline
value class Email(private val address: String) {
    init {
        val isValid = Pattern.compile(
            "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@" +
                    "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?" +
                    "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\." +
                    "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?" +
                    "[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|" +
                    "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$"
        ).matcher(address).matches()
        if (!isValid) {
            throw Exception("Email $address is wrong")
        }
    }

    override fun toString(): String {
        return address
    }
}
