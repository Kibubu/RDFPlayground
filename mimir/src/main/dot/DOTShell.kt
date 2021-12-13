package dot

import dot.colorTemplate.NodeColor
import org.apache.jena.atlas.io.IndentedWriter
import org.apache.jena.graph.Graph
import org.apache.jena.graph.Node
import org.apache.jena.graph.Triple
import org.apache.jena.riot.other.GLib
import org.apache.jena.riot.system.PrefixMap
import org.apache.jena.riot.system.PrefixMapFactory
import org.apache.jena.sparql.util.Context

open class DOTShell(
    protected var out: IndentedWriter,
    protected var prefixMap: PrefixMap = PrefixMapFactory.emptyPrefixMap(),
    protected var baseURI: String?,
    protected var context: Context?
) {
    private var graph: Graph = Graph.emptyGraph

    private fun writeGraphStart() {
        // Prints the start of the graph, and places new lines for readability
        out.print("digraph G{\n")
    }

    private fun writeGraphEnd() {
        // Prints end of graph, ending with empty line
        out.println("}")
    }

    fun writeGraphDOT(graph: Graph, colors: NodeColoring = NodeColor) {
        this.graph = graph
        // Writes graph on DOT syntax
        writeGraphStart()
        out.incIndent()
        out.println("charset=\"utf-8\";")
        out.println()

        // Get every Triple on the Graph
        val tripleIterator: Iterator<Triple> = graph.find(Node.ANY, Node.ANY, Node.ANY)
        // Get Nodes
        val nodesIterator: Iterator<Node> = listSubjectsAndObjects()

        out.println("// Edges")
        tripleIterator.forEach { triple: Triple ->
            // Prints a row for each Edge
            run {
                when {
                    triple.`object`.isLiteral ->
                        out.println(
                            "\"${ triple.subject }\" -> \"${ prettyTriple(triple) }\" " +
                            "[label=\"${ prettyLabel(triple.predicate) }\",color=\"${colors.literalEdge}\"]"
                        )
                    else ->
                        out.println(
                            "\"${ triple.subject }\" -> \"${ triple.`object` }\" " +
                            "[label=\"${ prettyLabel(triple.predicate) }\",color=\"${colors.normalEdge}\"]"
                        )
                }
            }
        }
        out.println()

        out.println("// Nodes")
        nodesIterator.forEach { node: Node ->
            run {
                when {
                    // Write a row for each node
                    node.isURI ->
                        out.println(
                            "\"${ node.uri }\" [label=\"${ prettyLabel(node) }\",shape=ellipse,color=\"${colors.uri}\"]"
                        )
                    node.isBlank ->
                        out.println(
                            "\"${node.blankNodeLabel}\" [label=\"\",shape=circle,color=\"${colors.blank}\"]"
                        )
                    node.isLiteral ->
                        when {
                            node.literal.value.toString().isNotEmpty() ->
                                out.println(
                                    "\"${prettyNode(node)}\" [label=\"${prettyLiteralLabel(node)}\"," +
                                            "shape=record,color=\"${colors.literal}\",${langLiteral(node)}" +
                                            "${dataTypeLiteral(node)}]"
                                )
                        }
                }
            }
        }

        out.decIndent()
        writeGraphEnd()
    }

    // Graph writing functions
    private fun listSubjects(): Iterator<Node> {
        return GLib.listSubjects(graph)
    }

    private fun listObjects(): Iterator<Node> {
        return GLib.listObjects(graph)
    }

    private fun listSubjectsAndObjects(): Iterator<Node> {
        val subjectsIterator: Iterator<Node> = listSubjects()
        val objectsIterator: Iterator<Node> = listObjects()

        val nodes = mutableListOf<Node>()
        // add all elements to a list
        subjectsIterator.forEach { aNode: Node -> nodes.add(aNode) }
        objectsIterator.forEach { aNode: Node -> nodes.add(aNode) }

        val filteredNodes = nodes.distinct()

        return filteredNodes.iterator()
    }

    private fun prettyLabel(node: Node): String {
        return when (node.prefix(prefixMap)) {
            null -> node.uri
            else -> "${node.prefix(prefixMap)}:${nodeLocalName(node)}"
        }
    }

    private fun nodeLocalName(node: Node): String {
        return node.getURI().substring(Math.max(node.getURI().lastIndexOf('/'),node.getURI().lastIndexOf('#'))+1)
    }

    private fun prettyTriple(triple: Triple): String {
        return when (triple.`object`.literalLanguage) {
            "" -> triple.`object`.literal.lexicalForm.toString().replace("\"", "\'")
            else -> triple.`object`.literal.lexicalForm.toString().replace("\"", "\'") +
                    "@" + triple.`object`.literalLanguage
        }
    }

    private fun prettyNode(node: Node): String {
        return when (node.literalLanguage) {
            "" -> node.literal.lexicalForm.toString().replace("\"", "\'")
            else -> node.literal.lexicalForm.toString().replace("\"", "\'") +
                    "@" + node.literalLanguage
        }
    }

    private fun prettyLiteralLabel(node: Node): String {
        return node.literal.lexicalForm.toString().replace("\"", "\'")
    }

    private fun langLiteral(node: Node): String {
        return when (node.literalLanguage) {
            "" -> ""
            else -> "lang= \"${node.literalLanguage}\","
        }
    }

    private fun dataTypeLiteral(node: Node): String {
        return "datatype= \"${node.literalDatatypeURI}\""
    }

}

// Auxiliary function that obtains a prefix for a given Node, must be URI
private fun Node.prefix(prefixMap: PrefixMap): String? {
    return when (prefixMap.abbrev(this.nameSpace)) {
        null -> null
        else -> prefixMap.abbrev(this.nameSpace).left
    }
}
