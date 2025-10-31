package dev.tmsoft.lib.query.filter

import org.jetbrains.exposed.v1.jdbc.Query


data class Field(
    val name: String,
    val function: Query.(value: List<Value>) -> Query,
    val values: List<String>
)
