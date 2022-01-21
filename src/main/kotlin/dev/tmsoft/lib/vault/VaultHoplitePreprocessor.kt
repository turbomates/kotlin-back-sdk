package dev.tmsoft.lib.vault

import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.PrimitiveNode
import com.sksamuel.hoplite.StringNode
import com.sksamuel.hoplite.parsers.toNode
import com.sksamuel.hoplite.preprocessor.TraversingPrimitivePreprocessor
import kotlinx.coroutines.runBlocking

class VaultHoplitePreprocessor(private val vaultAPI: VaultAPI) : TraversingPrimitivePreprocessor() {
    // example jdbc.database=vault:gambling/database
    private val regex = "vault:(.*?)/(.*?)".toRegex()

    override fun handle(node: PrimitiveNode): Node =
        when (node) {
            is StringNode ->
                when (val match = regex.matchEntire(node.value)) {
                    null ->
                        node

                    else -> {
                        val data = runBlocking {
                            vaultAPI.read(
                                match.groups[1]!!.value,
                                match.groups[2]!!.value
                            )
                        }

                        if (data.isEmpty()) {
                            node
                        } else {
                            process(data.toNode("vault"))
                        }
                    }
                }
            else -> {
                node
            }
        }
}
