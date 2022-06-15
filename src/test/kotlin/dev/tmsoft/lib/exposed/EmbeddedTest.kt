package dev.tmsoft.lib.exposed

import dev.tmsoft.lib.Config
import dev.tmsoft.lib.exposed.dao.Column
import dev.tmsoft.lib.exposed.dao.EmbeddableColumn
import dev.tmsoft.lib.exposed.dao.Embedded
import dev.tmsoft.lib.exposed.dao.EmbeddedTable
import dev.tmsoft.lib.exposed.dao.PrimitiveColumn
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

object Accounts : IntIdTable() {
    val name = varchar("account", 255).nullable()
    val balance = money()
    val latBalance = money("last_")
}

class MoneyColumn(table: Table, prefix: String = "") : EmbeddableColumn<Money>(table, prefix) {
    val amount = column(MoneyColumn.amount)
    val currency = column(MoneyColumn.currency)

    override fun instance(parts: Map<Column<*>, Any?>): Money {
        return Money(parts[MoneyColumn.amount] as Int, parts[MoneyColumn.currency] as Currency)
    }

    companion object : EmbeddedTable() {
        val amount = column { prefix -> integer(prefix + "amount") }
        val currency = compositeColumn { prefix -> CurrencyColumn(this, prefix) }
    }
}

fun Table.money(prefix: String = ""): MoneyColumn {
    return MoneyColumn(this, prefix)
}

class CurrencyColumn(table: Table, prefix: String = "") : EmbeddableColumn<Currency>(table, prefix) {
    val name = column(CurrencyColumn.name)
    override fun instance(parts: Map<Column<*>, Any?>): Currency {
        return Currency(parts[CurrencyColumn.name] as String)
    }

    companion object : EmbeddedTable() {
        val name = PrimitiveColumn { prefix -> varchar(prefix + "name", 25) }
    }
}

class Currency(name: String) : Embedded() {
    var name: String by CurrencyColumn.name

    init {
        this.name = name
    }
}

class Money constructor(amount: Int, currency: Currency) : Embedded() {
    var amount: Int by MoneyColumn.amount
    var currency: Currency by MoneyColumn.currency

    init {
        this.amount = amount
        this.currency = currency
    }
}

class Account(id: EntityID<Int>) : Entity<Int>(id) {
    var name by Accounts.name
    var balance by Accounts.balance
    var lastBalance by Accounts.latBalance

    companion object : EntityClass<Int, Account>(Accounts)
}

class EmbeddedTest {

    @Test
    fun changes() {
        val database = Database.connect(Config.h2DatabaseUrl, driver = Config.h2Driver, user = Config.h2User, password = Config.h2Password)
        transaction(database) {
            SchemaUtils.create(Accounts)
            val money = Money(10, Currency("EUR"))
            val last = Money(20, Currency("EUR"))
            Account.new {
                name = "test"
                balance = money
                lastBalance = last
            }
            var result = Account.wrapRows(Accounts.selectAll()).first()
            assertSame(10, result.balance.amount)
            assertSame("EUR", result.balance.currency.name)
            result.lastBalance = Money(40, Currency("TEST"))
            result = Account.wrapRows(Accounts.selectAll()).first()
            assertSame(40, result.lastBalance.amount)
        }
    }

    @Test
    fun selections() {
        val database = Database.connect(Config.h2DatabaseUrl, driver = Config.h2Driver, user = Config.h2User, password = Config.h2Password)
        transaction(database) {
            SchemaUtils.create(Accounts)
            val money = Money(10, Currency("EUR"))
            val last = Money(20, Currency("EUR"))
            Account.new {
                name = "test"
                balance = money
                lastBalance = last
            }
            var result = Account.wrapRow(Accounts.select { Accounts.balance.amount less 20 }.first())
            assertSame(20, result.lastBalance.amount)
            result = Account.wrapRow(Accounts.select { Accounts.balance.currency.name eq "EUR" }.first())
            assertSame("EUR", result.lastBalance.currency.name)
        }
    }
}
