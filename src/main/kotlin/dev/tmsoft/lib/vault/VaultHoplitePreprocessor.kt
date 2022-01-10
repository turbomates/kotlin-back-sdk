package dev.tmsoft.lib.vault

import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.PrimitiveNode
import com.sksamuel.hoplite.StringNode
import com.sksamuel.hoplite.parsers.toNode
import com.sksamuel.hoplite.preprocessor.TraversingPrimitivePreprocessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class VaultHoplitePreprocessor(private val vaultAPI: VaultAPI) : TraversingPrimitivePreprocessor(),
    CoroutineScope by CoroutineScope(Dispatchers.IO) {
    // example jdbc.database=vault:cubbyhole/database
    private val regex = "vault:(.*?)/(.*?)".toRegex()

    override fun handle(node: PrimitiveNode): Node = runBlocking {
        when (node) {
            is StringNode ->
                when (val match = regex.matchEntire(node.value)) {
                    null ->
                        node

                    else -> {

                        val data =
                            match.let { m -> vaultAPI.read(m.groups[1]!!.value, m.groups[2]!!.value) }
                        data.toNode("vault")
                    }
                }
            else -> {
                node
            }
        }
    }
}
