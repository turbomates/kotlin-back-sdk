package dev.tmsoft.lib.exposed

import dev.tmsoft.lib.exposed.type.JsonBContains
import dev.tmsoft.lib.exposed.type.jsonb
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JsonBTest {
    @Test
    fun column() {
        transaction(testDatabase) {
            SchemaUtils.create(JsonBTable)
            transaction {
                JsonBTable.insert {
                    it[name] = "test"
                    it[content] = listOf(Content("content", 1))
                }
            }
            val content = JsonBTable.selectAll().first()[JsonBTable.content].first().name
            assertEquals("content", content)
            SchemaUtils.drop(JsonBTable)
        }
    }

    @Test
    fun select() {
        transaction(testDatabase) {
            SchemaUtils.create(JsonBTable)
            transaction {
                JsonBTable.insert {
                    it[name] = "test"
                    it[content] = listOf(Content("content", 1), Content("content", 2))
                }
            }
            val content =
                JsonBTable.select {
                    JsonBContains(
                        JsonBTable.content,
                        JsonBTable.content.wrap(listOf(Content("content", 1)))
                    )
                }.single()[JsonBTable.content].first().name
            assertEquals("content", content)
            SchemaUtils.drop(JsonBTable)
        }
    }
}

object JsonBTable : IntIdTable("test_jsonb") {
    val name = varchar("account", 255).nullable()
    val content = jsonb("current_content", ListSerializer(Content.serializer()))
}

@Serializable
data class Content(val name: String, val order: Int)
