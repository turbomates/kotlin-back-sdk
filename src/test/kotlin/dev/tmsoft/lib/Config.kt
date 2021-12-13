package dev.tmsoft.lib

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.Masked
import com.sksamuel.hoplite.PropertySource
import dev.tmsoft.lib.config.hoplite.EnvironmentVariablesPropertySource

object Config {
    const val h2DatabaseUrl = "jdbc:h2:mem:test"
    const val h2Driver = "org.h2.Driver"
    const val h2User = "root"
    const val h2Password = ""
}

data class ConfigJdbc(val jdbc: Jdbc)
data class Jdbc(val url: String, val user: String, val password: Masked)
fun buildConfiguration(): Jdbc {
    return ConfigLoader.Builder()
        .addSource(PropertySource.resource("/local.properties", optional = true))
        .addSource(PropertySource.resource("/default.properties", optional = true))
        .addSource(EnvironmentVariablesPropertySource(true, true))
        .build()
        .loadConfigOrThrow<ConfigJdbc>().jdbc
}
