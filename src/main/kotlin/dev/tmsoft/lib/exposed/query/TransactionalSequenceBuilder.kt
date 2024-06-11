package dev.tmsoft.lib.exposed.query

import com.google.inject.Inject
import dev.tmsoft.lib.exposed.TransactionManager
import java.time.OffsetDateTime
import org.jetbrains.exposed.sql.Transaction

class TransactionalSequenceBuilder @Inject constructor(val transactional: TransactionManager) {
    @Suppress("ForbiddenMethodCall")
    inline fun <reified T : SequenceEntity> build(
        limit: Int = 5_000,
        crossinline batch: Transaction.(limit: Int, createdAt: OffsetDateTime?) -> List<T>
    ): Sequence<T> {
        var collection: List<T> = listOf()
        return sequence {
            do {
                val lastFetchedEntity = collection.lastOrNull()?.createdAt()
                collection = transactional.readOnlySync { batch(limit, lastFetchedEntity) }
                yieldAll(collection)
            } while (collection.isNotEmpty())
        }
    }
}

interface SequenceEntity {
    fun createdAt(): OffsetDateTime
}
