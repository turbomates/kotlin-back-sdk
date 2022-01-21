package dev.tmsoft.lib.config.hoplite

import com.sksamuel.hoplite.ArrayNode
import com.sksamuel.hoplite.MapNode
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.PrimitiveNode
import com.sksamuel.hoplite.preprocessor.Preprocessor

class PrefixRemovalPreprocessor(private val prefix: String) : Preprocessor {
    override fun process(node: Node): Node {
        val newNode = when (node) {
            is MapNode -> {
                val nodeMap = when (val prefixNode = node.map[prefix]) {
                    is MapNode -> node.map + prefixNode.map
                    else -> node.map
                }

                MapNode(nodeMap.map { (k, v) -> k to process(v) }.toMap(), node.pos, node.value)
            }
            is ArrayNode -> ArrayNode(node.elements.map { process(it) }, node.pos)
            is PrimitiveNode -> node
            else -> node
        }

        return newNode
    }
}
