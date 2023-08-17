@file:Suppress("UnstableApiUsage")

rootProject.name = "dev.tmsoft.lib"

enableFeaturePreview("VERSION_CATALOGS")
dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            version("ktor", "2.3.0")
            version("jedis", "4.2.3")
            version("log4j", "2.17.0")
            version("detekt", "1.21.0-RC1")
            version("hoplite", "2.1.5")
            version("kotlin", "1.9.0")
            version("s3", "0.25.0-beta")
            version("hikaricp", "5.0.1")
            version("exposed", "0.41.1")
            version("test_logger", "3.2.0")
            version("h2database", "1.4.200")
            version("google_guice", "5.1.0")
            version("valiktor_core", "0.12.0")
            version("postgresql_jdbc", "42.4.0")
            version("swagger_webjar", "4.1.3")
            version("embedded_postgres", "0.13.4")
            version("rabbitmq_amqp_client", "5.14.2")
            version("kotlin_serialization_json", "1.3.3")
            version("email", "1.6.2")
            version("greenmail", "1.6.9")
            version("tmsoft_openapi", "0.5.2")
            version("tmsoft_time", "0.1.0")

            alias("greenmail").to("com.icegreen", "greenmail-junit5").versionRef("greenmail")
            alias("email").to("com.sun.mail", "javax.mail").versionRef("email")
            alias("ktor_server_auth").to("io.ktor", "ktor-server-auth").versionRef("ktor")
            alias("ktor_server_webjars").to("io.ktor", "ktor-server-webjars").versionRef("ktor")
            alias("ktor_server_auth_jwt").to("io.ktor", "ktor-server-auth-jwt").versionRef("ktor")
            alias("ktor_server_locations").to("io.ktor", "ktor-server-locations").versionRef("ktor")
            alias("ktor_server_core").to("io.ktor", "ktor-server-core").versionRef("ktor")
            alias("ktor_server_sessions").to("io.ktor", "ktor-server-sessions").versionRef("ktor")
            alias("ktor_server_test_host").to("io.ktor", "ktor-server-test-host").versionRef("ktor")
            alias("ktor_client_cio").to("io.ktor", "ktor-client-cio").versionRef("ktor")
            alias("ktor_client_serialization").to("io.ktor", "ktor-client-serialization").versionRef("ktor")
            alias("exposed_time").to("org.jetbrains.exposed", "exposed-java-time").versionRef("exposed")
            alias("exposed_core").to("org.jetbrains.exposed", "exposed-core").versionRef("exposed")
            alias("exposed_jdbc").to("org.jetbrains.exposed", "exposed-jdbc").versionRef("exposed")
            alias("exposed_dao").to("org.jetbrains.exposed", "exposed-dao").versionRef("exposed")
            alias("postgresql_jdbc").to("org.postgresql", "postgresql").versionRef("postgresql_jdbc")
            alias("kotlin_test").to("org.jetbrains.kotlin", "kotlin-test-junit5").versionRef("kotlin")
            alias("embedded_postgres").to("com.opentable.components", "otj-pg-embedded").versionRef("embedded_postgres")
            alias("kotlin_serialization_json").to("org.jetbrains.kotlinx", "kotlinx-serialization-json")
                .versionRef("kotlin_serialization_json")
            alias("kotlin_serialization").to("org.jetbrains.kotlin", "kotlin-serialization").versionRef("kotlin")
            alias("rabbitmq_amqp_client").to("com.rabbitmq", "amqp-client").versionRef("rabbitmq_amqp_client")
            alias("log4j_slf4j").to("org.apache.logging.log4j", "log4j-slf4j-impl").versionRef("log4j")
            alias("kotlin_reflect").to("org.jetbrains.kotlin", "kotlin-reflect").versionRef("kotlin")
            alias("valiktor_core").to("org.valiktor", "valiktor-core").versionRef("valiktor_core")
            alias("swagger_webjar").to("org.webjars", "swagger-ui").versionRef("swagger_webjar")
            alias("log4j_core").to("org.apache.logging.log4j", "log4j-core").versionRef("log4j")
            alias("log4j_api").to("org.apache.logging.log4j", "log4j-api").versionRef("log4j")
            alias("google_guice").to("com.google.inject", "guice").versionRef("google_guice")
            alias("hoplite").to("com.sksamuel.hoplite", "hoplite-core").versionRef("hoplite")
            alias("h2_database").to("com.h2database", "h2").versionRef("h2database")
            alias("hikaricp").to("com.zaxxer", "HikariCP").versionRef("hikaricp")
            alias("jedis").to("redis.clients", "jedis").versionRef("jedis")
            alias("s3").to("aws.sdk.kotlin", "s3").versionRef("s3")

            alias("tmsoft_openapi").to("com.turbomates.ktor", "openapi").versionRef("tmsoft_openapi")
            alias("tmsoft_time").to("com.github.turbomates", "kotlin-time").versionRef("tmsoft_time")

            bundle(
                "turbomates", listOf(
                "tmsoft_openapi",
                "tmsoft_time"
            )
            )

            bundle(
                "ktor_client", listOf(
                "ktor_client_cio",
                "ktor_client_serialization"
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
                "exposed", listOf(
                "exposed_dao",
                "exposed_time",
                "exposed_core",
                "exposed_jdbc"
            )
            )

            alias("kotlin_serialization").toPluginId("org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
            alias("test_logger").toPluginId("com.adarshr.test-logger").versionRef("test_logger")
            alias("detekt").toPluginId("io.gitlab.arturbosch.detekt").versionRef("detekt")
            alias("detekt_formatting").to("io.gitlab.arturbosch.detekt", "detekt-formatting").versionRef("detekt")
        }
    }
}
