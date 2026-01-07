package dev.tmsoft.lib.ktor

import dev.tmsoft.lib.validation.Error
import org.valiktor.Constraint
import org.valiktor.DefaultConstraintViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.valiktor.functions.hasSize

class ValidationTest {
    data class TestDataClass(val prop: String)
    data class TestConstraint(val parameter: String) : Constraint {
        override val name = "test.name"
    }

    @Test
    fun `test validation constraint to error`() {
        val errors = setOf(
            DefaultConstraintViolation(
                "prop",
                "value",
                TestConstraint("parameter-value")
            )
        ).toErrorsList()
        assertEquals(1, errors.size)
        assertEquals(Error(
            message = "test.name",
            property = "prop",
            value = "value",
            parameters = mapOf("parameter" to "parameter-value")
        ), errors[0])
    }

    @Test
    fun `test validate with using valiktor constraints`() {
        val data = TestDataClass("test")

        runBlocking {
            val errors = validate(data) {
                validate(TestDataClass::prop).hasSize(5)
            }
            assertEquals(Error(
                message = "validation.size",
                property = "prop",
                value = "test",
                parameters = mapOf("min" to 5)
            ), errors[0])
        }
    }

    @Test
    fun `test validate with using custom constraint`() {
        val data = TestDataClass("test")

        runBlocking {
            val errors = validate(data) {
                validate(TestDataClass::prop).coValidate(TestConstraint("parameter-value")) { false }
            }
            assertEquals(Error(
                message = "test.name",
                property = "prop",
                value = "test",
                parameters = mapOf("parameter" to "parameter-value")
            ), errors[0])
        }
    }
}
