package dev.tmsoft.lib.exposed

import dev.tmsoft.lib.buildConfiguration
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database


internal val testDatabase by lazy {
    val config = buildConfiguration()
    Database.connect(
        config.url,
        user = config.user,
        password = config.password.value,
        driver = "org.postgresql.Driver",
        databaseConfig = DatabaseConfig { useNestedTransactions = true },

    )
}

