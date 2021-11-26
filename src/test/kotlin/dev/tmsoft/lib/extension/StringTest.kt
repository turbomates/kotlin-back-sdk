package dev.tmsoft.lib.extension

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StringTest {
    @Test
    fun `camel case to snake case`() {
        val camelCaseString = "exampleCamelCase"
        val snakeCaseString = camelCaseString.camelToSnakeCase()
        assertEquals("example_camel_case", snakeCaseString)
    }

    @Test
    fun `snake case to camel case`() {
        val camelCaseString = "example_camel_case"
        val snakeCaseString = camelCaseString.snakeToLowerCamelCase()
        assertEquals("exampleCamelCase", snakeCaseString)
    }

    @Test
    fun `snake case to camel case with capitalized first letter`() {
        val camelCaseString = "example_camel_case"
        val snakeCaseString = camelCaseString.snakeToUpperCamelCase()
        assertEquals("ExampleCamelCase", snakeCaseString)
    }
}
