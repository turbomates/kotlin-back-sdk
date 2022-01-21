package dev.tmsoft.lib.vault

import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.PropertySourceContext
import com.sksamuel.hoplite.fp.valid
import com.sksamuel.hoplite.parsers.toNode
import kotlinx.coroutines.runBlocking
import java.util.Properties

class VaultHopliteSource(
    private val vaultAPI: VaultAPI,
    private val namespace: String,
    private val key: String,
    private val separator: String = "."
) : PropertySource {
    override fun node(context: PropertySourceContext): ConfigResult<Node> {
        val props = Properties()
        val data = runBlocking {
            vaultAPI.read(namespace, key)
        }
        data.forEach {
            val key = it.key.split(separator).joinToString(separator = ".").lowercase()
            props[key] = it.value
        }

        return props.toNode("vault").valid()
    }
}
