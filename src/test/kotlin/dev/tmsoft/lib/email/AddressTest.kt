package dev.tmsoft.lib.email

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions


class AddressTest {
    @Test
    fun `valid address test`() {
        val emailAddr = Address("noreply@rx.casino")
        assertEquals("noreply@rx.casino", emailAddr.email)
    }
    @Test
    fun `invalid address test`() {
        Assertions.assertThrows(InvalidEmailAddress::class.java) {
            Address("norepl,y@rx.casino")
        }
    }
}
