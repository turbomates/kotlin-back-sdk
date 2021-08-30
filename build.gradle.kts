import com.adarshr.gradle.testlogger.theme.ThemeType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin(KotlinModules.jvm).version(Versions.kotlin) apply true
    id(Plugins.ktlint_gradle).version(Versions.ktlint_gradle)
    kotlin(Plugins.kotlin_serialization).version(Versions.kotlin)
    id(Plugins.test_logger) version Versions.test_logger
    id("maven-publish")
    signing
}

group = "dev.tmsoft.lib"
version = "0.1.15"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    api(Deps.ktor_locations)
    api(Deps.kotlin_reflect)
    api(Deps.ktor_server_core)
    api(Deps.ktor_auth_jwt)
    api(Deps.valiktor_core)
    api(Deps.google_guice)
    api(Deps.postgresqlJDBC)
    api(Deps.exposed_core)
    api(Deps.exposed_dao)
    api(Deps.exposed_jdbc)
    api(Deps.exposed_jdbc)
    api(Deps.exposed_time)
    api(Deps.kotlin_serialization_json)
    api(Deps.rabbitmq_amqp_client)
    api(Deps.swagger_webjar)
    api(Deps.ktor_webjar)
    api(Deps.s3)
    api(Deps.ktor_client_cio)
    api(Deps.ktor_client_serialization)
    api(Deps.jedis)
    runtimeOnly(Deps.logback_classic)

    testImplementation(Deps.kotlin_test)
    testImplementation(Deps.ktor_server_test_host)
    testImplementation(Deps.junit_jupiter_api)
    testImplementation(Deps.embedded_postgres)
    testImplementation(Deps.h2_database)
    testRuntimeOnly(Deps.junit_jupiter_engine)
}

ktlint {
    version.set("0.41.0")
    debug.set(false)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(true)
    enableExperimentalRules.set(false)
    disabledRules.set(setOf("import-ordering"))
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }

    filter {
        include("**/kotlin/**")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "15"
        freeCompilerArgs = listOf(
            "-Xuse-experimental=io.ktor.locations.KtorExperimentalLocationsAPI",
            "-Xuse-experimental=kotlin.ExperimentalStdlibApi",
            "-Xuse-experimental=kotlinx.serialization.InternalSerializationApi",
            "-Xuse-experimental=kotlinx.serialization.ExperimentalSerializationApi",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xlambdas=indy"
        )
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_15
}

//  ----------------  TEST ----------------  //

testlogger {
    theme = ThemeType.PLAIN
}
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("PASSED", "STARTED", "FAILED", "SKIPPED")
        // showStandardStreams = true
    }
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "common-lib"
            groupId = "dev.tmsoft.kotlin"
            from(components["java"])
            pom {
                name.set("Kotlin backend common library")
                description.set("This library contains different resolutions with ktor, exposed, serialization")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("shustrik")
                        name.set("vadim golodko")
                        email.set("vadim@turbomates.com")
                    }
                }
                scm {
                    connection.set("scm:https://github.com/turbomates/kotlin-back-sdk.git")
                    developerConnection.set("scm:git@github.com:turbomates/kotlin-back-sdk.git")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/turbomates/kotlin-back-sdk")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
