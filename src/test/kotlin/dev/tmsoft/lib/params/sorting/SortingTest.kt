package dev.tmsoft.lib.params.sorting

import dev.tmsoft.lib.Config
import dev.tmsoft.lib.params.PathValues
import dev.tmsoft.lib.params.SingleValue
import dev.tmsoft.lib.params.paging.PagingParameters
import dev.tmsoft.lib.params.paging.sorting.Sorting
import dev.tmsoft.lib.params.paging.toContinuousList
import java.time.LocalDate
import kotlin.test.assertTrue
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
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
        val database = Database.connect(
            Config.h2DatabaseUrl,
            Config.h2Driver,
            Config.h2User,
            Config.h2Password
        )

        transaction(database) {
            SchemaUtils.create(UserTable)
            for (i in 1..5) {
                UserTable.insert {
                    it[name] = "test"
                    it[number] = i
                    it[modifyAt] = LocalDate.now()
                }
            }

            val users = UserTable.selectAll()
                .toContinuousList(
                    PagingParameters(30, 1),
                    ResultRow::toUser,
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
                users.data.first().order == 5 &&
                    users.data.last().order == 1
            )

            Assertions.assertThrows(IllegalArgumentException::class.java) {
                UserTable.selectAll()
                    .toContinuousList(
                        PagingParameters(30, 1),
                        ResultRow::toUser,
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
data class User(
    val name: String,
    val order: Int
)

fun ResultRow.toUser() = User(
    this[UserTable.name],
    this[UserTable.number]
)
