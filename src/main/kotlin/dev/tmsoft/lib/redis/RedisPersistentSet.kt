package dev.tmsoft.lib.redis

import dev.tmsoft.lib.logger.logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RedisPersistentSet(
    private val access: Access,
    private val prefix: String? = null,
    val serializer: Json = Json,
) {
    private val logger = logger()

    fun add(key: String, value: String) {
        logger.debug("Add value for key: {}, value: {}", key, value)
        access.sadd(prefixedKey(key), value)
    }

    @JvmName("AddSerializable")
    inline fun <reified T> add(key: String, value: T) {
        add(key, serializer.encodeToString(value))
    }

    fun get(key: String): Set<String> {
        logger.debug("Get values for key: {}", key)
        return access.smembers(prefixedKey(key))
    }

    @JvmName("GetSerializable")
    inline fun <reified T> get(key: String): Set<T> = get(key).map { serializer.decodeFromString<T>(it) }.toSet()

    @Suppress("SpreadOperator")
    fun remove(key: String, values: List<String>) {
        logger.debug("Remove for key: {}, values: {}", key, values)
        access.srem(prefixedKey(key), *values.toTypedArray())
    }

    @JvmName("RemoveSerializable")
    inline fun <reified T> remove(key: String, values: List<T>) {
        remove(key, values.map { serializer.encodeToString(it) })
    }

    fun size(key: String): Long {
        return access.scard(key)
    }

    private fun prefixedKey(key: String) = prefix?.let { "$prefix$key" } ?: key
}
