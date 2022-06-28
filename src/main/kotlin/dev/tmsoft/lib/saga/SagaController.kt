package dev.tmsoft.lib.saga

import com.google.inject.Inject
import dev.tmsoft.lib.event.Event
import dev.tmsoft.lib.event.EventsSubscriber
import dev.tmsoft.lib.exposed.TransactionManager

class SagaController @Inject constructor(val transaction: TransactionManager) : EventsSubscriber {
    private val sagaStorage = SagaStorage()
//    private val sagas: MutableMap<Saga.Key, List<SagaSubscriber<out Saga.Data>>> = mutableMapOf()

    suspend fun <TSaga : Saga.Data> run(saga: Saga<TSaga>, block: suspend () -> Unit) {
        try {
            transaction {
                block()
                sagaStorage.save(saga)
            }
        } catch (ignore: Exception) {
            rollback(saga)
            throw ignore
        }
    }

    suspend fun <TSaga : Saga.Data> rollback(saga: Saga<TSaga>) {
        transaction {
            sagaStorage.save(saga)
        }
    }

    override fun subscribers(): List<EventsSubscriber.EventSubscriberItem<out Event>> {
        TODO("Not yet implemented")
    }
}

// class SyncSagaController @Inject constructor(
//    private val sagaController: SagaController,
// ) : Controller by sagaController {
//    override suspend fun <TSaga : Saga> run(saga: TSaga, block: suspend () -> Void) {
//        try {
//            sagaController.transaction {
//                block()
//            }
//        } catch (ex: Exception) {
//            rollback(saga)
//            throw  ex
//        }
//    }
// }
//
//private interface Controller {
//    suspend fun <T : Saga> run(command: T, block: suspend () -> Void)
//    fun <T : Saga> rollback(saga: T)
//    fun <TSaga : Saga, THandler : SagaHandler<TSaga>> register(
//        subscriber: Subscriber<THandler>,
//        saga: KClass<out TSaga>
//    )
//}
//
