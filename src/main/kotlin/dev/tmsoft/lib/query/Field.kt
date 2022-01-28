package dev.tmsoft.lib.query

import org.jetbrains.exposed.sql.Query

data class Field(
    val name: String,
    val function: Query.(value: List<Value>) -> Query,
    val values: List<String>
)
