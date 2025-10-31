package dev.tmsoft.lib

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.Masked
import com.sksamuel.hoplite.addResourceSource
import com.sksamuel.hoplite.sources.EnvironmentVariablesPropertySource

object Config {
    const val h2DatabaseUrl = "jdbc:h2:mem:test;MODE=MySQL"
    const val h2Driver = "org.h2.Driver"
    const val h2User = "root"
    const val h2Password = ""
}

data class ConfigJdbc(val jdbc: Jdbc)
data class Jdbc(val url: String, val user: String, val password: Masked)
fun buildConfiguration(): Jdbc {
    return ConfigLoaderBuilder.default()
        .addResourceSource("/local.properties", optional = true)
        .addResourceSource("/default.properties", optional = true)
        .addSource(EnvironmentVariablesPropertySource(useUnderscoresAsSeparator = true, allowUppercaseNames = true))
        .build()
        .loadConfigOrThrow<ConfigJdbc>().jdbc
}
