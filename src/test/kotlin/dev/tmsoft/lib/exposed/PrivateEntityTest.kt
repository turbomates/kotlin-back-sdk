package dev.tmsoft.lib.exposed

import com.turbomates.time.exposed.CurrentTimestamp
import com.turbomates.time.exposed.datetime
import dev.tmsoft.lib.Config
import dev.tmsoft.lib.exposed.dao.PrivateEntityClass
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertNotNull
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test

class BetTicket private constructor(id: EntityID<UUID>) : UUIDEntity(id) {
    val pick by BetTicketPick referrersOn BetTicketPickTable.betTicketId
    var rejectReason by BetTicketTable.rejectReason
    var updatedAt by BetTicketTable.updatedAt

    companion object : PrivateEntityClass<UUID, BetTicket>(object : Repository() {})

    abstract class Repository : EntityClass<UUID, BetTicket>(BetTicketTable, BetTicket::class.java) {
        override fun createInstance(entityId: EntityID<UUID>, row: ResultRow?): BetTicket {
            return BetTicket(entityId)
        }
    }
}

class BetTicketPick private constructor(id: EntityID<UUID>) : UUIDEntity(id) {
    var betTicket by BetTicket referencedOn BetTicketPickTable.betTicketId
    var selectionId by BetTicketPickTable.selectionId

    companion object : PrivateEntityClass<UUID, BetTicketPick>(object : Repository() {}) {
    }

    abstract class Repository : EntityClass<UUID, BetTicketPick>(BetTicketPickTable, BetTicketPick::class.java) {
        override fun createInstance(entityId: EntityID<UUID>, row: ResultRow?): BetTicketPick {
            return BetTicketPick(entityId)
        }
    }
}

object BetTicketTable : UUIDTable("sportsbook_bets_tickets") {
    val rejectReason = text("reject_reason").nullable()
    val updatedAt = datetime("updated_at").defaultExpression(CurrentTimestamp())
}

object BetTicketPickTable : UUIDTable("sportsbook_bets_tickets_picks") {
    val betTicketId = reference("bet_ticket_id", BetTicketTable)
    val selectionId = uuid("selection_id")
}

object Repository : BetTicketPick.Repository()

class PrivateEntityTest {
    @Test
    fun reference() {
        val database = Database.connect(
            Config.h2DatabaseUrl,
            driver = Config.h2Driver,
            user = Config.h2User,
            password = Config.h2Password
        )
        transaction(database) {
            SchemaUtils.create(BetTicketTable)
            SchemaUtils.create(BetTicketPickTable)
            val betTicket = BetTicket.new {
                rejectReason = "test"
                updatedAt = OffsetDateTime.now()
            }
            BetTicketPick.new {
                this.betTicket = betTicket
                selectionId = UUID.randomUUID()
            }
            flushCache()
            val result = Repository.wrapRows(BetTicketTable.join(BetTicketPickTable, JoinType.LEFT).selectAll()).first()
            assertNotNull(result.betTicket)
        }

    }

}
