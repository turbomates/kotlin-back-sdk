package dev.tmsoft.lib.ktor

import org.valiktor.Constraint
import org.valiktor.DefaultConstraintViolation
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidationTest {
    data class TestConstraint(val parameter: String) : Constraint {
        override val name = "name"
    }

    @Test
    fun `test validation constraint to error`() {
        val errors = setOf(DefaultConstraintViolation("prop", "value", TestConstraint("parameter-value"))).toErrorsList()
        assertEquals(1, errors.size)
        val error = errors.first()
        assertEquals("value", error.value)
        assertEquals("prop", error.property)
        assertEquals("name", error.message)
        assertEquals(1, error.parameters?.size)
        assertEquals("parameter-value", error.parameters?.get("parameter"))
    }
}
