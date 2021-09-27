import com.adarshr.gradle.testlogger.theme.ThemeType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin(KotlinModules.jvm).version(Versions.kotlin) apply true
    id(Plugins.detekt).version(Versions.detekt)
    kotlin(Plugins.kotlin_serialization).version(Versions.kotlin)
    id(Plugins.test_logger) version Versions.test_logger
    id("maven-publish")
    signing
}

group = "dev.tmsoft.lib"
version = "0.2.11"

repositories {
    mavenCentral()
}

dependencies {
    api(Deps.ktor_locations)
    api(Deps.kotlin_reflect)
    api(Deps.ktor_server_sessions)
    api(Deps.ktor_serialization)
    api(Deps.ktor_server_core)
    api(Deps.ktor_auth_jwt)
    api(Deps.ktor_auth)
    api(Deps.valiktor_core)
    api(Deps.google_guice)
    api(Deps.postgresqlJDBC)
    api(Deps.hikaricp)
    api(Deps.exposed_core)
    api(Deps.exposed_dao)
    api(Deps.exposed_jdbc)
    api(Deps.exposed_jdbc)
    api(Deps.exposed_time)
    api(Deps.kotlin_serialization)
    api(Deps.kotlin_serialization_json)
    api(Deps.rabbitmq_amqp_client)
    api(Deps.swagger_webjar)
    api(Deps.ktor_webjar)
    api(Deps.s3)
    api(Deps.hoplite)
    api(Deps.ktor_client_cio)
    api(Deps.ktor_client_serialization)
    api(Deps.jedis)
    api(Deps.log4j_api)
    api(Deps.log4j_slf4j)
    runtimeOnly(Deps.log4j_core)

    testImplementation(Deps.kotlin_test)
    testImplementation(Deps.ktor_server_test_host)
    testImplementation(Deps.h2_database)
    testImplementation(Deps.junit_jupiter_api)
    testImplementation(Deps.embedded_postgres)
    testRuntimeOnly(Deps.junit_jupiter_engine)
}

detekt {
    toolVersion = Versions.detekt
    ignoreFailures = false
    parallel = true
    allRules = false
    config = files("detekt.yml")
    buildUponDefaultConfig = true
    reports {
        xml.enabled = true
        html.enabled = false
        txt.enabled = false
        sarif.enabled = false
    }
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "15"
        freeCompilerArgs = listOf(
            "-Xopt-in=io.ktor.locations.KtorExperimentalLocationsAPI",
            "-Xopt-in=kotlin.ExperimentalStdlibApi",
            "-Xopt-in=kotlinx.serialization.InternalSerializationApi",
            "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.time.ExperimentalTime",
            "-Xlambdas=indy"
        )
    }
}

configure<JavaPluginExtension> {
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
