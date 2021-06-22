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
version = "0.1.2"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(Deps.kotlin_reflect)
    implementation(Deps.ktor_locations)
    implementation(Deps.ktor_server_core)
    implementation(Deps.ktor_auth_jwt)
    implementation(Deps.valiktor_core)
    implementation(Deps.google_guice)
    implementation(Deps.postgresqlJDBC)
    implementation(Deps.exposed_core)
    implementation(Deps.exposed_dao)
    implementation(Deps.exposed_jdbc)
    implementation(Deps.exposed_time)
    implementation(Deps.kotlin_serialization_json)
    implementation(Deps.rabbitmq_amqp_client)
    implementation(Deps.embedded_postgres)
    implementation(Deps.swagger_webjar)
    implementation(Deps.ktor_webjar)
    implementation(Deps.s3)
    runtimeOnly(Deps.logback_classic)

    testImplementation(Deps.ktor_server_test_host)
    testImplementation(Deps.junit_jupiter_api)
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


