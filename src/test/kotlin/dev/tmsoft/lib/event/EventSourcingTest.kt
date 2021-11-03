//package dev.tmsoft.lib.event
//
//import dev.tmsoft.lib.event.EventSourcingAccess
//import dev.tmsoft.lib.event.EventsDatabaseAccess
//import dev.tmsoft.lib.exposed.TransactionManager
//import io.betforge.TestEventSubscribers
//import io.betforge.createAccount
//import io.betforge.createUser
//import io.betforge.infrastructure.domain.Currency
//import io.betforge.infrastructure.domain.Money
//import io.betforge.rollbackTransaction
//import io.betforge.sportsbook.application.BetViewSubscriber
//import io.betforge.sportsbook.infrastructure.Repository
//import io.betforge.sportsbook.model.bet.Bet
//import io.betforge.sportsbook.model.bet.Selection
//import io.betforge.sportsbook.model.bet.SingleBet
//import io.betforge.sportsbook.model.event.BetPlaced
//import io.betforge.sportsbook.model.event.BetPrepared
//import io.betforge.sportsbook.model.event.Owner
//import io.betforge.sportsbook.view.BetViewBuilder
//import io.betforge.testDatabase
//import kotlinx.coroutines.runBlocking
//import org.junit.jupiter.api.Assertions
//import org.junit.jupiter.api.Test
//
//class EventSourcingTest {
//
//    @Test
//    fun `event sourcing`() {
//        rollbackTransaction {
//            val user = testDatabase.createUser()
//            val account = testDatabase.createAccount(user)
//            val money = Money(1, Currency.EUR)
//            val bet = SingleBet()
//            val eventPrepared = BetPrepared(
//                bet.id,
//                Owner(user, account),
//                money,
//                listOf(Selection(1, 1.1)),
//                bet.type
//            )
//            val eventPlaced = BetPlaced(bet.id, account, money)
//            val transactionManager = TransactionManager(testDatabase)
//            val subscriber = TestEventSubscribers(EventsDatabaseAccess(transactionManager))
//            val repository = Repository(EventSourcingAccess(transactionManager))
//
//            subscriber.subscribe(BetViewSubscriber(BetViewBuilder(transactionManager)))
//
//            runBlocking {
//                subscriber.call(eventPrepared)
//                subscriber.call(eventPlaced)
//            }
//            //
//            // EventSourcingTable.batchInsert(listOf(eventPrepared, eventPlaced)) { event ->
//            //     this[EventSourcingTable.id] = UUID.randomUUID()
//            //     this[EventSourcingTable.event] = EventWrapper(event)
//            //     this[EventSourcingTable.aggregateRoot] = bet.id
//            // }
//
//            runBlocking {
//                val updatedBet = repository.get(bet.id)
//                Assertions.assertEquals(updatedBet.profit, Money(0, Currency.EUR))
//                Assertions.assertEquals(updatedBet.status, Bet.Status.ACTIVE)
//                Assertions.assertEquals(updatedBet.type, Bet.Type.SINGLE)
//                Assertions.assertEquals(updatedBet.stake, money)
//            }
//        }
//    }
//}
