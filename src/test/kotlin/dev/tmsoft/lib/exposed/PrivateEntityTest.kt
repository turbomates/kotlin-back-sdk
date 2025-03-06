package dev.tmsoft.lib.exposed

import com.turbomates.time.exposed.CurrentTimestamp
import com.turbomates.time.exposed.datetime
import dev.tmsoft.lib.exposed.dao.PrivateEntityClass
import java.util.UUID
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow

class BetTicket private constructor(id: EntityID<UUID>) : UUIDEntity(id) {
    private val picks by BetTicketPick referencedOn BetTicketPickTable.betTicketId
    private var rejectReason by BetTicketTable.rejectReason
    private var updatedAt by BetTicketTable.updatedAt
    companion object : PrivateEntityClass<UUID, BetTicket>(object : Repository() {}) {
    }

    abstract class Repository : EntityClass<UUID, BetTicket>(BetTicketTable, BetTicket::class.java) {
        override fun createInstance(entityId: EntityID<UUID>, row: ResultRow?): BetTicket {
            return BetTicket(entityId)
        }
    }
}

class BetTicketPick private constructor(id: EntityID<UUID>) : UUIDEntity(id) {
    private var betTicket by BetTicket referencedOn BetTicketTable.id
    private var selectionId by BetTicketPickTable.selectionId

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
