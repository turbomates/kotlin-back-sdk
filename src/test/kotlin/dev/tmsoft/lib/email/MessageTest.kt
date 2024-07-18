package dev.tmsoft.lib.email

import kotlin.test.Test
import kotlin.test.assertFailsWith

class MessageTest {
    @Test
    fun wrongEmails() {
        listOf(
            "dp@mayhem@partners"
        ).forEach { email ->
            assertFailsWith<InvalidEmailAddress> {
                Address(email)
            }
        }
    }

    @Test
    fun rightEmails() {
        listOf(
            "test@gmail.com",
            "dp@mayhem.partners"
        ).forEach { email ->
            Address(email)
        }
    }
}

