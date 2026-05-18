package dev.tmsoft.lib.query

import dev.tmsoft.lib.Config
import dev.tmsoft.lib.query.paging.PagingParameters
import dev.tmsoft.lib.query.paging.SortingParameter
import dev.tmsoft.lib.query.paging.sortingParameters
import dev.tmsoft.lib.query.paging.toContinuousList
import io.ktor.http.Parameters
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.QueryParameter
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.anyFrom
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.expandArgs
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ContinuousListTest {
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

            val users = runBlocking {
                UserTable.selectAll()
                    .toContinuousList(
                        PagingParameters(30, 1),
                        ResultRow::toUser,
                        listOf(
                            SortingParameter("number", SortOrder.DESC),
                            SortingParameter("name", SortOrder.ASC)
                        )
                    )

            }
            assertTrue(
                users.data.first().order == 5 &&
                        users.data.last().order == 1
            )


            Assertions.assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    UserTable.selectAll()
                        .toContinuousList(
                            PagingParameters(30, 1),
                            ResultRow::toUser,
                            listOf(
                                SortingParameter(
                                    "modifyAt",
                                    SortOrder.DESC
                                )
                            )
                        )

                }
            }

            val parameters = object : Parameters {
                override val caseInsensitiveName: Boolean = false
                override fun entries(): Set<Map.Entry<String, List<String>>> {
                    return mapOf("sorting[name]" to listOf("asc")).entries
                }

                override fun getAll(name: String): List<String> = emptyList()
                override fun isEmpty(): Boolean = false
                override fun names(): Set<String> = emptySet()
            }.sortingParameters().first()

            assertTrue {
                parameters.name == "name" &&
                        parameters.sortOrder == SortOrder.ASC
            }
        }
    }

    @Test
    fun `test count`() {
        val database = Database.connect(
            Config.h2DatabaseUrl,
            Config.h2Driver,
            Config.h2User,
            Config.h2Password
        )

        transaction(database) {
            SchemaUtils.create(UserTable)
            val count = 60
            for (i in 1..count) {
                UserTable.insert {
                    it[name] = "test"
                    it[number] = i
                    it[modifyAt] = LocalDate.now()
                }
            }

            val users = runBlocking {
                UserTable.selectAll()
                    .toContinuousList(
                        PagingParameters(30, 1),
                        ResultRow::toUser,
                        emptyList(),
                        true
                    )

            }
            assertEquals(users.count, count.toLong())
        }
    }

    @Test
    fun `postgres uniq count pagination with join`() {
        val database = Database.connect(
            Config.h2DatabaseUrl,
            Config.h2Driver,
            Config.h2User,
            Config.h2Password
        )
        transaction(database) {
            SchemaUtils.create(UserTable)
            SchemaUtils.create(AddressTable)
            val count = 39
            for (i in 1..count) {
                val user = UserTable.insertAndGetId {
                    it[name] = "test_$i"
                    it[number] = i
                    it[modifyAt] = LocalDate.now()
                }
                for (j in 1..5) {
                    AddressTable.insert {
                        it[address] = "address_$j"
                        it[this.user] = user.value
                        it[sequence] = i * j
                    }
                }
            }

            val users = runBlocking {
                UserTable
                    .join(AddressTable, JoinType.LEFT, AddressTable.user, UserTable.id)
                    .selectAll()
                    .toContinuousList(
                        PagingParameters(2, 1),
                        ResultRow::toUser,
                        listOf(
                            SortingParameter(
                                "sequence",
                                SortOrder.DESC
                            )
                        ),
                        true
                    )

            }
            assertEquals(count.toLong(), users.count)
        }
    }

    @Test
    fun `root query path - join paginated by root table column`() {
        val database = Database.connect(
            "jdbc:h2:mem:test_root_join;MODE=MySQL",
            Config.h2Driver, Config.h2User, Config.h2Password
        )
        transaction(database) {
            val sql = captureSql()

            SchemaUtils.create(UserTable, AddressTable)
            for (i in 1..5) {
                val userId = UserTable.insertAndGetId {
                    it[name] = "u$i"; it[number] = i; it[modifyAt] = LocalDate.now()
                }
                AddressTable.insert { it[user] = userId.value; it[address] = "a$i"; it[sequence] = i * 10 }
            }

            val page1 = runBlocking {
                UserTable.join(AddressTable, JoinType.LEFT, AddressTable.user, UserTable.id)
                    .selectAll()
                    .toContinuousList(
                        PagingParameters(2, 1), ResultRow::toUser,
                        listOf(SortingParameter("number", SortOrder.DESC))
                    )
            }
            assertEquals(2, page1.data.size)
            assertEquals(5, page1.data[0].order)
            assertEquals(4, page1.data[1].order)
            assertTrue(page1.hasMore)
            assertTrue(sql.none { "DISTINCT ON" in it }, "ID subquery must not contain DISTINCT ON")

            val page3 = runBlocking {
                UserTable.join(AddressTable, JoinType.LEFT, AddressTable.user, UserTable.id)
                    .selectAll()
                    .toContinuousList(
                        PagingParameters(2, 3), ResultRow::toUser,
                        listOf(SortingParameter("number", SortOrder.DESC))
                    )
            }
            assertEquals(1, page3.data.size)
            assertEquals(1, page3.data[0].order)
            assertFalse(page3.hasMore)
        }
    }

    @Test
    fun `root query path - exists filter on child table`() {
        val database = Database.connect(
            "jdbc:h2:mem:test_exists_filter;MODE=MySQL",
            Config.h2Driver, Config.h2User, Config.h2Password
        )
        transaction(database) {
            val sql = captureSql()

            SchemaUtils.create(UserTable, TagTable)
            for (i in 1..10) {
                val userId = UserTable.insertAndGetId {
                    it[name] = "u$i"; it[number] = i; it[modifyAt] = LocalDate.now()
                }
                if (i <= 5) {
                    TagTable.insert { it[user] = userId; it[label] = "premium" }
                }
            }

            val result = runBlocking {
                UserTable.join(TagTable, JoinType.LEFT, TagTable.user, UserTable.id)
                    .selectAll()
                    .where {
                        exists(
                            TagTable.select(TagTable.id)
                                .where { (TagTable.user eq UserTable.id) and (TagTable.label eq "premium") }
                        )
                    }
                    .toContinuousList(
                        PagingParameters(3, 1), ResultRow::toUser,
                        listOf(SortingParameter("number", SortOrder.ASC)), true
                    )
            }
            assertEquals(3, result.data.size)
            assertEquals(1, result.data[0].order)
            assertEquals(3, result.data[2].order)
            assertTrue(result.hasMore)
            assertEquals(5L, result.count)
            assertTrue(sql.none { "DISTINCT ON" in it }, "ID subquery must not contain DISTINCT ON")
        }
    }

    @Test
    fun `left join without where filter stays optional and does not exclude unmatched rows`() {
        val database = Database.connect(
            "jdbc:h2:mem:test_left_join_optional;MODE=MySQL",
            Config.h2Driver, Config.h2User, Config.h2Password
        )
        transaction(database) {
            SchemaUtils.create(UserTable, AddressTable, TagTable)

            // user 1: matches address filter AND has tag
            val u1 = UserTable.insertAndGetId { it[name] = "u1"; it[number] = 1; it[modifyAt] = LocalDate.now() }
            AddressTable.insert { it[user] = u1.value; it[address] = "premium"; it[sequence] = 10 }
            TagTable.insert { it[user] = u1; it[label] = "vip" }

            // user 2: matches address filter, NO tag — must still appear (TagTable LEFT JOIN is optional)
            val u2 = UserTable.insertAndGetId { it[name] = "u2"; it[number] = 2; it[modifyAt] = LocalDate.now() }
            AddressTable.insert { it[user] = u2.value; it[address] = "premium"; it[sequence] = 20 }

            // user 3: wrong address — must not appear
            val u3 = UserTable.insertAndGetId { it[name] = "u3"; it[number] = 3; it[modifyAt] = LocalDate.now() }
            AddressTable.insert { it[user] = u3.value; it[address] = "other"; it[sequence] = 30 }

            val result = runBlocking {
                UserTable
                    .join(AddressTable, JoinType.LEFT, AddressTable.user, UserTable.id)
                    .join(TagTable, JoinType.LEFT, TagTable.user, UserTable.id)
                    .selectAll()
                    .where { AddressTable.address eq "premium" }
                    .toContinuousList(
                        PagingParameters(10, 1), ResultRow::toUser,
                        listOf(SortingParameter("number", SortOrder.ASC))
                    )
            }

            assertEquals(2, result.data.size)
            assertEquals(1, result.data[0].order)
            assertEquals(2, result.data[1].order)
        }
    }

    @Test
    fun `distinct query path - sort on joined column orders correctly`() {
        val database = Database.connect(
            "jdbc:h2:mem:test_distinct_order;MODE=MySQL",
            Config.h2Driver, Config.h2User, Config.h2Password
        )
        transaction(database) {
            val sql = captureSql()

            SchemaUtils.create(UserTable, AddressTable)
            for (i in 1..4) {
                val userId = UserTable.insertAndGetId {
                    it[name] = "u$i"; it[number] = i; it[modifyAt] = LocalDate.now()
                }
                AddressTable.insert { it[user] = userId.value; it[address] = "a$i"; it[sequence] = i * 10 }
            }

            val result = runBlocking {
                UserTable.join(AddressTable, JoinType.LEFT, AddressTable.user, UserTable.id)
                    .selectAll()
                    .toContinuousList(
                        PagingParameters(2, 1), ResultRow::toUser,
                        listOf(SortingParameter("sequence", SortOrder.DESC))
                    )
            }
            assertEquals(2, result.data.size)
            assertEquals(4, result.data[0].order)
            assertEquals(3, result.data[1].order)
            assertTrue(result.hasMore)
            assertTrue(sql.any { "DISTINCT ON" in it }, "ID subquery must contain DISTINCT ON")
        }
    }
    @Test
    fun `where uses anyFrom over joined-table array column does not drop the join in id subquery`() {
        val database = Database.connect(
            "jdbc:h2:mem:test_anyfrom;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
            Config.h2Driver, Config.h2User, Config.h2Password
        )
        transaction(database) {
            val sql = captureSql()

            SchemaUtils.create(UserTable, ChannelTable)
            val users = (1..4).map { i ->
                UserTable.insertAndGetId {
                    it[name] = "u$i"; it[number] = i; it[modifyAt] = LocalDate.now()
                }
            }
            ChannelTable.insert {
                it[owner] = users[0]; it[subscribers] = listOf(users[1].value, users[2].value)
            }

            val targetId = users[1].value
            val result = runBlocking {
                UserTable
                    .join(ChannelTable, JoinType.INNER, UserTable.id, ChannelTable.owner)
                    .selectAll()
                    .where { QueryParameter(targetId, IntegerColumnType()) eq anyFrom(ChannelTable.subscribers) }
                    .toContinuousList(
                        PagingParameters(10, 1), ResultRow::toUser,
                        listOf(SortingParameter("number", SortOrder.ASC))
                    )
            }
            assertEquals(1, result.data.size)
            assertEquals(1, result.data[0].order)
            assertFalse(result.hasMore)
            val unjoinedReferences = sql.any { stmt ->
                stmt.startsWith("SELECT", ignoreCase = true) &&
                        stmt.contains("channel", ignoreCase = true) &&
                        !stmt.contains("JOIN", ignoreCase = true) &&
                        !stmt.contains("FROM channel", ignoreCase = true) &&
                        !stmt.contains("FROM \"channel", ignoreCase = true)
            }
            assertFalse(
                unjoinedReferences,
                "Generated SELECT must keep the channel table in FROM/JOIN if its column is referenced"
            )
        }
    }
}


object GroupTable : IntIdTable() {
    val address = reference("address_id", AddressTable)
    val groupName = varchar("group_name", 100)
}

object ChannelTable : IntIdTable() {
    val owner = reference("owner_id", UserTable)
    val subscribers = array("subscribers", IntegerColumnType())
}

private fun Transaction.captureSql(): MutableList<String> {
    val statements = mutableListOf<String>()
    addLogger(object : SqlLogger {
        override fun log(context: StatementContext, transaction: Transaction) {
            val sql = context.expandArgs(transaction)
            println("[SQL] $sql")
            statements.add(sql)
        }
    })
    return statements
}

object UserTable : IntIdTable() {
    val name = varchar("name", 255)
    val number = integer("number")
    val modifyAt = date("modify_at").default(LocalDate.now())
}

object AddressTable : IntIdTable() {
    val user = integer("user_id")
    val address = varchar("address", 255)
    val sequence = integer("sequence")
}

data class User(
    val name: String,
    val order: Int
)

fun ResultRow.toUser() = User(
    this[UserTable.name],
    this[UserTable.number]
)

object TagTable : IntIdTable() {
    val user = reference("user_id", UserTable)
    val label = varchar("label", 100)
}
