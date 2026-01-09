package dev.tmsoft.lib.ktor

import dev.tmsoft.lib.validation.Error
import org.valiktor.Constraint
import org.valiktor.DefaultConstraintViolation
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidationTest {
    data class CustomConstraint(val parameter: String) : Constraint {
        override val name = "custom.error_code"
    }

    data class StandardConstraint(val parameter: String) : Constraint

    @Test
    fun `test custom constraint with dot uses name as-is`() {
        val errors = setOf(
            DefaultConstraintViolation("prop", "value", CustomConstraint("parameter-value"))
        ).toErrorsList()
        assertEquals(1, errors.size)
        assertEquals(Error(
            message = "custom.error_code",
            property = "prop",
            value = "value",
            parameters = mapOf("parameter" to "parameter-value")
        ), errors[0])
    }

    @Test
    fun `test standard constraint gets validation prefix`() {
        val errors = setOf(
            DefaultConstraintViolation("prop", "value", StandardConstraint("parameter-value"))
        ).toErrorsList()
        assertEquals(1, errors.size)
        assertEquals(Error(
            message = "validation.standard_constraint",
            property = "prop",
            value = "value",
            parameters = mapOf("parameter" to "parameter-value")
        ), errors[0])
    }
}
