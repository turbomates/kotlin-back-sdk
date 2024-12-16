package dev.tmsoft.lib.ktor

import dev.tmsoft.lib.structure.Either
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

class ResponseTest {

    @Test
    fun `test ok response serialize`() {
        val json = Json { }
        assertEquals(JsonObject(mapOf("data" to JsonPrimitive("ok"))), json.encodeToJsonElement(Response.Ok))
    }

    @Test
    fun `test either response serialize`() {
        val json = Json { }
        assertEquals(
            JsonObject(mapOf("data" to JsonPrimitive("Test"))),
            json.encodeToJsonElement(
                Response.Either<Response.Ok, Response.Data<String>>(
                    Either.Right(
                        Response.Data("Test")
                    )
                )
            )
        )

        assertEquals(JsonNull, json.encodeToJsonElement(Response.Either<Response.Ok, Response.Empty>(Either.Right(Response.Empty))))
    }

    @Test
    fun `test data response serialize`() {
        val json = Json { }
        assertEquals(
            JsonObject(mapOf("data" to JsonPrimitive("Test"))),
            json.encodeToJsonElement(Response.Data<String>("Test"))
        )
    }

    @Test
    fun `test error response serialize`() {
        val json = Json { }
        assertEquals(
            JsonObject(
                mapOf(
                    "error" to JsonObject(
                        mapOf(
                            "message" to JsonPrimitive("error"),
                            "property" to JsonPrimitive("property"),
                            "value" to JsonPrimitive("value")
                        )
                    )
                )
            ),
            json.encodeToJsonElement(Response.Error(dev.tmsoft.lib.validation.Error("error", "property", "value")))
        )
    }

    @Test
    fun `test errors response serialize`() {
        val json = Json { }
        assertEquals(
            JsonObject(
                mapOf(
                    "errors" to JsonArray(
                        listOf(
                            JsonObject(
                                mapOf(
                                    "message" to JsonPrimitive("error"),
                                    "property" to JsonPrimitive("property"),
                                    "value" to JsonPrimitive("value"),
                                    "parameters" to JsonObject(mapOf("parameter" to JsonPrimitive("value")))
                                )
                            )
                        )
                    )
                )
            ),
            json.encodeToJsonElement(
                Response.Errors(
                    listOf(
                        dev.tmsoft.lib.validation.Error(
                            "error",
                            "property",
                            "value",
                            mapOf("parameter" to "value")
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `test empty response serialize`() {
        val json = Json { }
        assertEquals(JsonNull, json.encodeToJsonElement(Response.Empty))
    }
}
