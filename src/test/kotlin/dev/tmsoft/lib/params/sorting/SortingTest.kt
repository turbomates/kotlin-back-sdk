package dev.tmsoft.lib.params.sorting

import dev.tmsoft.lib.Config
import dev.tmsoft.lib.params.PathValues
import dev.tmsoft.lib.params.SingleValue
import java.time.LocalDate
import kotlin.test.assertTrue
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SortingTest {
    @Test
    fun `test sorting`() {
        val database = Database.connect(Config.h2DatabaseUrl, driver = Config.h2Driver, user = Config.h2User, password = Config.h2Password)

        transaction(database) {
            SchemaUtils.create(UserTable)
            UserTable.insert {
                it[name] = "test"
                it[number] = 1
                it[modifyAt] = LocalDate.now()
            }
            val query = UserTable.selectAll()
                .sortedBy(
                    UserSorting,
                    PathValues(
                        mapOf(
                            "order" to listOf(SingleValue("desc")),
                            "name" to listOf(SingleValue("ASC")),
                            "modifyAt" to listOf(SingleValue("DESC"))
                        )
                    )
                )
            assertTrue(
                query.prepareSQL(this)
                    .contains("ORDER BY \"USER\".\"NUMBER\" DESC, \"USER\".\"NAME\" ASC")
            )

            Assertions.assertThrows(IllegalArgumentException::class.java) {
                UserTable.selectAll()
                    .sortedBy(
                        UserSorting,
                        PathValues(
                            mapOf(
                                "order" to listOf(SingleValue("descqw")),
                            )
                        )
                    )
            }
        }
    }
}

object UserSorting : Sorting(UserTable) {
    val order = add("order", UserTable.number)
    val name = add("name")
}

object UserTable : IntIdTable() {
    val name = varchar("name", 255)
    val number = integer("number")
    val modifyAt = date("modify_at").default(LocalDate.now())
}
