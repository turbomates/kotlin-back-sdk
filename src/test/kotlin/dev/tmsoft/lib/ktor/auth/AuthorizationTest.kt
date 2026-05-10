package dev.tmsoft.lib.ktor.auth

import io.ktor.server.application.install
import io.ktor.server.application.plugin
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthorizationTest {

    @Test
    fun `authorize throws for empty activities`() = testApplication {
        application {
            install(Authorization)
            routing {
                authorize(emptySet()) {
                    get("/test") { }
                }
            }
        }

        assertFailsWith<IllegalArgumentException> {
            startApplication()
        }
    }

    @Test
    fun `buildMap contains activities for authorized route`() = testApplication {
        lateinit var rules: RouteAuthorizationRules
        lateinit var protectedRoute: Route

        application {
            install(Authorization)
            routing {
                protectedRoute = authorize(setOf("report.read")) {
                    get("/reports") { }
                }
            }
        }

        startApplication()
        rules = application.plugin(Authorization).rules()
        rules.addRule(protectedRoute, setOf("report.read"))

        val map = rules.buildMap()
        val key = map.keys.firstOrNull { it.startsWith("GET:/reports") }
        assertNotNull(key, "Expected rules map to contain GET:/reports route")
        assertTrue(map.getValue(key).contains("report.read"))
    }

    @Test
    fun `nested authorize merges activities from parent and child`() = testApplication {
        lateinit var rules: RouteAuthorizationRules
        lateinit var protectedRoute: Route

        application {
            install(Authorization)
            routing {
                protectedRoute = authorize(setOf("project.read")) {
                    authorize(setOf("project.write")) {
                        post("/projects") { }
                    }
                }
            }
        }

        startApplication()
        rules = application.plugin(Authorization).rules()
        rules.addRule(protectedRoute, setOf("project.read", "project.write"))

        val map = rules.buildMap()
        val key = map.keys.firstOrNull { it.startsWith("POST:/projects") }
        assertNotNull(key, "Expected rules map to contain POST:/projects route")
        assertTrue(map.getValue(key).containsAll(setOf("project.read", "project.write")))
    }
}
