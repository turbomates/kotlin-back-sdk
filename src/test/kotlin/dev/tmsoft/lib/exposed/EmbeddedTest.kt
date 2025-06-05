package dev.tmsoft.lib.exposed

import dev.tmsoft.lib.Config
import dev.tmsoft.lib.exposed.QueryFoldTest.ExtraneousTable.nullable
import dev.tmsoft.lib.exposed.dao.Column
import dev.tmsoft.lib.exposed.dao.EmbeddableColumn
import dev.tmsoft.lib.exposed.dao.Embedded
import dev.tmsoft.lib.exposed.dao.EmbeddedTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

object Accounts : IntIdTable() {
    val name = varchar("account", 255).nullable()
    val balance = money()
    val lastBalance = money("last_")
    val bonusBalance = money("bonus_").nullable()
}

class MoneyColumn<T : Money?>(table: Table, prefix: String = "") : EmbeddableColumn<T>(table, prefix) {
    val amount = column(MoneyColumn.amount)
    val currency = column(MoneyColumn.currency)
    private var isNullable: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun instance(parts: Map<Column<*>, Any?>): T {
        val instance = if (isNullable) {
            val amount = parts[MoneyColumn.amount] as? Int?
            val currency = parts[MoneyColumn.currency] as? Currency?
            amount?.let { currency?.let { Money(amount, currency) } }
        } else Money(parts[MoneyColumn.amount] as Int, parts[MoneyColumn.currency] as Currency)

        return instance as T
    }

    @Suppress("UNCHECKED_CAST")
    fun nullable() = apply {
        amount.nullable()
        currency.nullable()
        isNullable = true
    } as MoneyColumn<T?>

    companion object : EmbeddedTable() {
        val amount = column { prefix -> integer(prefix + "amount") }
        val currency = column { prefix -> varchar(prefix + "currency", 5) }
    }
}

fun Table.money(prefix: String = ""): MoneyColumn<Money> {
    return MoneyColumn(this, prefix)
}

typealias Currency = String

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
    var lastBalance by Accounts.lastBalance
    var bonusBalance by Accounts.bonusBalance

    companion object : EntityClass<Int, Account>(Accounts)
}

class EmbeddedTest {

    @Test
    fun nullable() {
        val database = Database.connect(Config.h2DatabaseUrl, driver = Config.h2Driver, user = Config.h2User, password = Config.h2Password)
        transaction(database) {
            SchemaUtils.create(Accounts)
            val money = Money(10, "EUR")
            val last = Money(20, "EUR")
            val account = Account.new {
                name = "test"
                balance = money
                lastBalance = last
                bonusBalance = money
            }
            val result = Account.wrapRows(Accounts.selectAll()).first()
            assertNotNull(result.bonusBalance)
            result.bonusBalance?.let { assertSame(10, it.amount) }

            account.bonusBalance = null
            assertNull(Account.wrapRows(Accounts.selectAll()).first().bonusBalance)
        }
    }

    @Test
    fun changes() {
        val database = Database.connect(Config.h2DatabaseUrl, driver = Config.h2Driver, user = Config.h2User, password = Config.h2Password)
        transaction(database) {
            SchemaUtils.create(Accounts)
            val money = Money(10, "EUR")
            val last = Money(20, "EUR")
            Account.new {
                name = "test"
                balance = money
                lastBalance = last
                bonusBalance = null
            }
            var result = Account.wrapRows(Accounts.selectAll()).first()
            assertSame(10, result.balance.amount)
            assertSame("EUR", result.balance.currency)
            result.lastBalance = Money(40, "TEST")
            result = Account.wrapRows(Accounts.selectAll()).first()
            assertSame(40, result.lastBalance.amount)
            assertNull(result.bonusBalance)
        }
    }

    @Test
    fun selections() {
        val database = Database.connect(Config.h2DatabaseUrl, driver = Config.h2Driver, user = Config.h2User, password = Config.h2Password)
        transaction(database) {
            SchemaUtils.create(Accounts)
            val money = Money(10, "EUR")
            val last = Money(20, "EUR")
            Account.new {
                name = "test"
                balance = money
                lastBalance = last
                bonusBalance = null
            }
            var result = Account.wrapRow(Accounts.selectAll().where { Accounts.balance.amount less 20 }.first())
            assertSame(20, result.lastBalance.amount)
            result = Account.wrapRow(Accounts.selectAll().where { Accounts.balance.currency eq "EUR" }.first())
            assertSame("EUR", result.lastBalance.currency)
            assertNull(result.bonusBalance)
        }
    }
}
