package dev.tmsoft.lib.query.filter

import dev.tmsoft.lib.Config.h2DatabaseUrl
import dev.tmsoft.lib.Config.h2Driver
import dev.tmsoft.lib.Config.h2Password
import dev.tmsoft.lib.Config.h2User
import dev.tmsoft.lib.exposed.Money
import dev.tmsoft.lib.exposed.money
import java.time.LocalDate
import kotlin.test.assertTrue
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.OrOp
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.joinQuery
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test

class FilterTest {
    @Test
    fun `simple query`() {
        val database = Database.connect(h2DatabaseUrl, driver = h2Driver, user = h2User, password = h2Password)
        transaction(database) {
            SchemaUtils.create(UserTable)
            val money = Money(10, "EUR")
            UserTable.insert {
                it[name] = "test"
                it[number] = 1
                it[balance] = money
                it[modifyAt] = LocalDate.now()
            }

            val query = UserTable.selectAll()
                .filter(
                    UserFilter,
                    PathValues(
                        mapOf(
                            "full_name" to listOf(SingleValue("Test"), SingleValue("Test2")),
                            "number" to listOf(RangeValue("1", null)),
                            "custom" to listOf(RangeValue("2021-12-12", null)),
                            "join" to listOf(RangeValue("address", null)),
                            "modify_at" to listOf(RangeValue("2021-11-11", "2021-11-12"))
                        )
                    )
                )

            println( query.prepareSQL(this))
            assertTrue(
                query.prepareSQL(this).lowercase()
                    .contains("((LOWER(\"USER\".FULL_NAME) LIKE ?) OR (LOWER(\"USER\".FULL_NAME) LIKE ?)) AND (\"USER\".\"NUMBER\" >= ?) AND (\"USER\".MODIFY_AT >= ?) AND ((LOWER(PROFILE.ADDRESS) LIKE ?)) AND (\"USER\".MODIFY_AT >= ?) AND (\"USER\".MODIFY_AT <= ?)".lowercase())
            )
        }
    }

    @Test
    fun `list query`() {
        val database = Database.connect(h2DatabaseUrl, driver = h2Driver, user = h2User, password = h2Password)
        transaction(database) {
            SchemaUtils.create(UserTable)
            val money = Money(10, "EUR")
            UserTable.insert {
                it[name] = "test"
                it[number] = 1
                it[balance] = money
                it[modifyAt] = LocalDate.now()
            }

            UserTable.insert {
                it[name] = "test_2"
                it[number] = 2
                it[balance] = money
                it[modifyAt] = LocalDate.now()
            }
            val query = UserTable.selectAll()
                .filter(
                    UserFilter,
                    PathValues(
                        mapOf(
                            "full_name" to listOf(ListValue(listOf(SingleValue("test"), SingleValue("test_2")))),
                            "number" to listOf(ListValue(listOf(SingleValue("1"), SingleValue("2"))))
                        )
                    )
                )
            assertTrue(
                 query.prepareSQL(this).lowercase()
                    .contains("((LOWER(\"USER\".FULL_NAME) LIKE ?) OR (LOWER(\"USER\".FULL_NAME) LIKE ?)) AND ((\"USER\".\"NUMBER\" = ?) OR (\"USER\".\"NUMBER\" = ?))".lowercase())
            )
        }
    }
    @Test
    fun `simple enum query`() {
        val database = Database.connect(h2DatabaseUrl, driver = h2Driver, user = h2User, password = h2Password)
        transaction(database) {
            SchemaUtils.create(TestEnumTable)
            TestEnumTable.insert {
                it[status] = TestEnum.ACTIVE
            }
            val query = TestEnumTable.selectAll()
                .filter(
                    TestEnumFilter,
                    PathValues(
                        mapOf(
                            "status_test" to listOf(SingleValue("ACTIVE")),
                        )
                    )
                )
            assertTrue(
                query.prepareSQL(this).lowercase()
                    .contains("SELECT TESTENUM.ID, TESTENUM.STATUS FROM TESTENUM WHERE TESTENUM.STATUS = ?".lowercase())
            )
        }
    }
}

object UserFilter : Filter(UserTable) {
    val name = add("full_name")
    val number = add("number")
    val custom = add("custom", UserTable.modifyAt)
    val join = add("join", ProfileTable.address) { values ->
        addJoin {
            join(
                ProfileTable,
                JoinType.LEFT,
                ProfileTable.user,
                UserTable.id
            )
        }.andWhere { OrOp(values.map { it.op(ProfileTable.address) }) }
    }
    val join2 = add("join2", ProfileTable.address) { values ->
        addJoin {
            join(
                ProfileTable,
                JoinType.LEFT,
                ProfileTable.user,
                UserTable.id
            )
        }.andWhere { OrOp(values.map { it.op(ProfileTable.address) }) }
    }
    val modifyAt = add("modify_at")
}

object UserTable : IntIdTable() {
    val name = varchar("full_name", 255)
    val number = integer("number")
    val balance = money()
    val modifyAt = date("modify_at").default(LocalDate.now())
}

object ProfileTable : IntIdTable() {
    val user = reference("user", UserTable)
    val address = varchar("address", 255)
    val phone = varchar("phone", 255)
}
object TestEnumTable : IntIdTable() {
    val status = enumerationByName<TestEnum>("status", 255)
}
object TestEnumFilter : Filter(TestEnumTable) {
    val name = add("status_test", TestEnumTable.status)
}
enum class TestEnum {
    ACTIVE, INACTIVE
}
