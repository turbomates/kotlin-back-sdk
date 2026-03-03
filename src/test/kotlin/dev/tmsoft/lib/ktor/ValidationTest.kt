package dev.tmsoft.lib.ktor

import dev.tmsoft.lib.validation.Error
import kotlinx.coroutines.runBlocking
import org.valiktor.Constraint
import org.valiktor.DefaultConstraintViolation
import org.valiktor.functions.isNotNull
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidationTest {
    data class CustomConstraint(val parameter: String) : Constraint {
        override val name = "custom.error_code"
    }

    data class TestCommand(
        val title: String?,
        val description: String?
    )

    @Test
    fun `test custom constraint with dot uses name as-is`() {
        val errors = setOf(
            DefaultConstraintViolation("myProperty", "value", CustomConstraint("parameter-value"))
        ).toErrorsList()
        assertEquals(1, errors.size)
        assertEquals(
            Error(
                key = "custom.error_code",
                namespace = "my_property",
                property = "myProperty",
                value = "value",
                parameters = mapOf("parameter" to "parameter-value")
            ),
            errors[0]
        )
    }

    @Test
    fun `test with form namespace`() {
        val errors = setOf(
            DefaultConstraintViolation("title", "value", CustomConstraint("param"))
        ).toErrorsList("bonus.update_achievement")
        assertEquals(1, errors.size)
        assertEquals(
            Error(
                key = "custom.error_code",
                namespace = "bonus.update_achievement.title",
                property = "title",
                value = "value",
                parameters = mapOf("parameter" to "param")
            ),
            errors[0]
        )
    }

    @Test
    fun `test validate function with isNotNull constraint`() = runBlocking {
        val command = TestCommand(title = null, description = "test")
        val errors = validate(command, "bonus.create_achievement") {
            validate(TestCommand::title).isNotNull()
        }
        assertEquals(1, errors.size)
        assertEquals("validation.not_null", errors[0].key)
        assertEquals("bonus.create_achievement.title", errors[0].namespace)
        assertEquals("title", errors[0].property)
    }

    @Test
    fun `test validate function with multiple errors`() = runBlocking {
        val command = TestCommand(title = null, description = null)
        val errors = validate(command, "player.register") {
            validate(TestCommand::title).isNotNull()
            validate(TestCommand::description).isNotNull()
        }
        assertEquals(2, errors.size)
        assertEquals("player.register.title", errors[0].namespace)
        assertEquals("player.register.description", errors[1].namespace)
    }

    @Test
    fun `test validate function without namespace`() = runBlocking {
        val command = TestCommand(title = null, description = "test")
        val errors = validate(command) {
            validate(TestCommand::title).isNotNull()
        }
        assertEquals(1, errors.size)
        assertEquals("title", errors[0].namespace)
        assertEquals("title", errors[0].property)
    }
}
