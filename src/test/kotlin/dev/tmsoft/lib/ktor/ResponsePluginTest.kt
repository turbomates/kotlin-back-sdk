package dev.tmsoft.lib.ktor

import dev.tmsoft.lib.query.paging.ContinuousList
import dev.tmsoft.lib.structure.Either
import dev.tmsoft.lib.validation.Error
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

class ResponsePluginTest {

    @Serializable
    data class TestData(val value: String)

    @Test
    fun `test Response_Ok sets status to 200`() = testApplication {
        application {
            install(ResponsePlugin)
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/ok") {
                    call.respond(Response.Ok)
                }
            }
        }

        val response = client.get("/ok")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test Response_Empty sets status to 204`() = testApplication {
        application {
            install(ResponsePlugin)
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/empty") {
                    call.respond(Response.Empty)
                }
            }
        }

        val response = client.get("/empty")
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `test Response_Data sets status to 200`() = testApplication {
        application {
            install(ResponsePlugin)
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/data") {
                    call.respond(Response.Data(TestData("test-value")))
                }
            }
        }

        val response = client.get("/data")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test Response_Error sets status to 422 when current status is OK`() = testApplication {
        application {
            install(ResponsePlugin)
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/error") {
                    call.respond(Response.Error(Error("error message", "field", "invalid")))
                }
            }
        }

        val response = client.get("/error")
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
    }

    @Test
    fun `test Response_Errors sets status to 422 when current status is OK`() = testApplication {
        application {
            install(ResponsePlugin)
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/errors") {
                    call.respond(
                        Response.Errors(
                            listOf(
                                Error("error 1", "field1", "value1"),
                                Error("error 2", "field2", "value2")
                            )
                        )
                    )
                }
            }
        }

        val response = client.get("/errors")
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
    }

    @Test
    fun `test Response_Listing sets status to 200`() = testApplication {
        application {
            install(ResponsePlugin)
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/listing") {
                    call.respond(
                        Response.Listing(
                            ContinuousList(
                                data = listOf(TestData("item1"), TestData("item2")),
                                pageSize = 30,
                                currentPage = 1,
                                hasMore = false
                            )
                        )
                    )
                }
            }
        }

        val response = client.get("/listing")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test Response_Redirect performs redirect`() = testApplication {
        application {
            install(ResponsePlugin)
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/redirect") {
                    call.respond(Response.Redirect("/target"))
                }
                get("/target") {
                    call.respond("redirected")
                }
            }
        }

        // Note: By default, the test client follows redirects
        val response = client.get("/redirect")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("redirected", response.bodyAsText())
    }

    @Test
    fun `test Response_File serves file with status 200`() = testApplication {
        application {
            install(ResponsePlugin)
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/file") {
                    // Create a temporary file for testing
                    val tempFile = File.createTempFile("test", ".txt")
                    tempFile.writeText("file content")
                    tempFile.deleteOnExit()

                    call.respond(Response.File(tempFile))
                }
            }
        }

        val response = client.get("/file")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("file content", response.bodyAsText())
    }

    @Test
    fun `test Response_Either with Right value uses right response status`() = testApplication {
        application {
            install(ResponsePlugin)
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    serializersModule = SerializersModule {
                        contextual(Response.Data::class, ResponseDataSerializer)
                        contextual(Response.Either::class, ResponseEitherSerializer)
                        contextual(Response.Listing::class, ResponseListingSerializer)
                    }
                })
            }
            routing {
                get("/either-right") {
                    call.respond(
                        Response.Either<Response.Error, Response.Data<TestData>>(
                            Either.Right(Response.Data(TestData("success")))
                        )
                    )
                }
            }
        }

        val response = client.get("/either-right")
        println(response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test Response_Either with Left value uses left response status`() = testApplication {
        application {
            install(ResponsePlugin)
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/either-left") {
                    call.respond(
                        Response.Either<Response.Error, Response.Data<TestData>>(
                            Either.Left(Response.Error(Error("validation failed", "field", "value")))
                        )
                    )
                }
            }
        }

        val response = client.get("/either-left")
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
    }

    @Test
    fun `test Response_Error preserves custom status code`() = testApplication {
        application {
            install(ResponsePlugin)
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/error-custom") {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respond(Response.Error(Error("bad request", null, null)))
                }
            }
        }

        val response = client.get("/error-custom")
        // Should preserve BadRequest instead of changing to UnprocessableEntity
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `test status function for Response_Empty`() {
        val response = Response.Empty
        assertEquals(HttpStatusCode.NoContent, response.status(null))
        assertEquals(HttpStatusCode.NoContent, response.status(HttpStatusCode.OK))
    }

    @Test
    fun `test status function for Response_Error with OK status`() {
        val response = Response.Error(Error("error", null, null))
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status(null))
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status(HttpStatusCode.OK))
    }

    @Test
    fun `test status function for Response_Error with custom status`() {
        val response = Response.Error(Error("error", null, null))
        assertEquals(HttpStatusCode.BadRequest, response.status(HttpStatusCode.BadRequest))
        assertEquals(HttpStatusCode.Forbidden, response.status(HttpStatusCode.Forbidden))
    }

    @Test
    fun `test status function for Response_Data`() {
        val response = Response.Data(TestData("test"))
        assertEquals(HttpStatusCode.OK, response.status(null))
        assertEquals(HttpStatusCode.Created, response.status(HttpStatusCode.Created))
    }

    @Test
    fun `test status function for Response_Ok`() {
        val response = Response.Ok
        assertEquals(HttpStatusCode.OK, response.status(null))
        assertEquals(HttpStatusCode.OK, response.status(HttpStatusCode.Created))
    }
}
