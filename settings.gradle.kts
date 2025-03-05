@file:Suppress("UnstableApiUsage")

rootProject.name = "dev.tmsoft.lib"

dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            version("ktor", "2.3.13")
            version("jedis", "5.2.0")
            version("log4j", "2.24.3")
            version("detekt", "1.23.6")
            version("hoplite", "2.7.5")
            version("kotlin", "2.1.10")
            version("s3", "1.1.0")
            version("hikaricp", "6.2.1")
            version("exposed", "0.60.0")
            version("test_logger", "3.2.0")
            version("h2database", "2.3.232")
            version("google_guice", "7.0.0")
            version("valiktor_core", "0.12.0")
            version("postgresql_jdbc", "42.7.5")
            version("swagger_webjar", "4.15.5")
            version("embedded_postgres", "0.13.4")
            version("rabbitmq_amqp_client", "5.25.0")
            version("kotlin_serialization_json", "1.8.0")
            version("email", "1.6.2")
            version("greenmail", "1.6.9")
            version("tmsoft_openapi", "0.5.2")
            version("tmsoft_time", "0.1.2")
            version("sentry", "7.19.0")
            version("opentelemetry", "1.45.0")
            version("prometheus_micrometer", "1.11.0")
            version("slf4j_coroutines", "1.10.1")

            library("h2_database", "com.h2database", "h2").versionRef("h2database")
            library("greenmail", "com.icegreen", "greenmail-junit5").versionRef("greenmail")
            library("email", "com.sun.mail", "javax.mail").versionRef("email")
            library("ktor_server_auth", "io.ktor", "ktor-server-auth").versionRef("ktor")
            library("ktor_server_webjars", "io.ktor", "ktor-server-webjars").versionRef("ktor")
            library("ktor_server_auth_jwt", "io.ktor", "ktor-server-auth-jwt").versionRef("ktor")
            library("ktor_server_locations", "io.ktor", "ktor-server-locations").versionRef("ktor")
            library("ktor_server_core", "io.ktor", "ktor-server-core").versionRef("ktor")
            library("ktor_server_sessions", "io.ktor", "ktor-server-sessions").versionRef("ktor")
            library("ktor_server_test_host", "io.ktor", "ktor-server-test-host").versionRef("ktor")
            library("ktor_client_content_negotiation", "io.ktor", "ktor-client-content-negotiation").versionRef("ktor")
            library("ktor_client_cio", "io.ktor", "ktor-client-cio").versionRef("ktor")
            library("ktor_client_serialization", "io.ktor", "ktor-client-serialization").versionRef("ktor")
            library("exposed_time", "org.jetbrains.exposed", "exposed-java-time").versionRef("exposed")
            library("exposed_core", "org.jetbrains.exposed", "exposed-core").versionRef("exposed")
            library("exposed_jdbc", "org.jetbrains.exposed", "exposed-jdbc").versionRef("exposed")
            library("exposed_dao", "org.jetbrains.exposed", "exposed-dao").versionRef("exposed")
            library("postgresql_jdbc", "org.postgresql", "postgresql").versionRef("postgresql_jdbc")
            library("kotlin_test", "org.jetbrains.kotlin", "kotlin-test-junit5").versionRef("kotlin")
            library("embedded_postgres", "com.opentable.components", "otj-pg-embedded").versionRef("embedded_postgres")
            library("kotlin_serialization_json", "org.jetbrains.kotlinx", "kotlinx-serialization-json")
                .versionRef("kotlin_serialization_json")
            library("ktor_serialization_kotlinx_json", "io.ktor", "ktor-serialization-kotlinx-json").versionRef("ktor")
            library("kotlin_serialization", "org.jetbrains.kotlin", "kotlin-serialization").versionRef("kotlin")
            library("rabbitmq_amqp_client", "com.rabbitmq", "amqp-client").versionRef("rabbitmq_amqp_client")
            library("log4j_slf4j", "org.apache.logging.log4j", "log4j-slf4j-impl").versionRef("log4j")
            library("kotlin_reflect", "org.jetbrains.kotlin", "kotlin-reflect").versionRef("kotlin")
            library("valiktor_core", "org.valiktor", "valiktor-core").versionRef("valiktor_core")
            library("swagger_webjar", "org.webjars", "swagger-ui").versionRef("swagger_webjar")
            library("log4j_core", "org.apache.logging.log4j", "log4j-core").versionRef("log4j")
            library("log4j_api", "org.apache.logging.log4j", "log4j-api").versionRef("log4j")
            library("google_guice", "com.google.inject", "guice").versionRef("google_guice")
            library("hoplite", "com.sksamuel.hoplite", "hoplite-core").versionRef("hoplite")
            library("hikaricp", "com.zaxxer", "HikariCP").versionRef("hikaricp")
            library("jedis", "redis.clients", "jedis").versionRef("jedis")
            library("s3", "aws.sdk.kotlin", "s3").versionRef("s3")
            library("detekt_formatting", "io.gitlab.arturbosch.detekt", "detekt-formatting").versionRef("detekt")
            library("opentelemetry-api", "io.opentelemetry", "opentelemetry-api").versionRef("opentelemetry")
            library("opentelemetry-sdk", "io.opentelemetry", "opentelemetry-sdk").versionRef("opentelemetry")
            library("opentelemetry-coroutines", "io.opentelemetry", "opentelemetry-extension-kotlin").versionRef(
                "opentelemetry"
            )
            library("tmsoft_openapi", "com.turbomates.ktor", "openapi").versionRef("tmsoft_openapi")
            library("tmsoft_time", "com.github.turbomates", "kotlin-time").versionRef("tmsoft_time")
            library("prometheus_micrometer", "io.micrometer", "micrometer-registry-prometheus").versionRef(
                "prometheus_micrometer"
            )
            library("slf4j_coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-slf4j")
                .versionRef("slf4j_coroutines")
            plugin("kotlin_serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
            plugin("test_logger", "com.adarshr.test-logger").versionRef("test_logger")
            plugin("detekt", "io.gitlab.arturbosch.detekt").versionRef("detekt")
            library("sentry","io.sentry", "sentry").versionRef("sentry")
            bundle(
                "turbomates", listOf(
                    "tmsoft_openapi",
                    "tmsoft_time"
                )
            )

            bundle(
                "ktor_client", listOf(
                    "ktor_client_cio",
                    "ktor_client_serialization",
                    "ktor_client_content_negotiation",
                    "ktor_serialization_kotlinx_json"
                )
            )

            bundle(
                "ktor_server", listOf(
                    "ktor_server_auth",
                    "ktor_server_webjars",
                    "ktor_server_auth_jwt",
                    "ktor_server_locations",
                    "ktor_server_core",
                    "ktor_server_sessions"
                )
            )
            bundle(
                "opentelemetry",
                listOf(
                    "opentelemetry-api",
                    "opentelemetry-sdk",
                    "opentelemetry-coroutines",
                )
            )
            bundle(
                "exposed", listOf(
                    "exposed_dao",
                    "exposed_time",
                    "exposed_core",
                    "exposed_jdbc"
                )
            )
        }
    }
}
