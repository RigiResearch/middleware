package com.rigiresearch.middleware.historian.templates

import com.rigiresearch.middleware.graph.Graph
import com.rigiresearch.middleware.graph.Input
import com.rigiresearch.middleware.graph.Node
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.UUID

/**
 * Generates a DOT specification based on a graph instance.
 *
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @date 2019-07-27
 * @version $Id$
 * @since 0.0.1
 */
final class GraphTemplate {

    /**
     * Creates a file containing a DOT specification based on the given
     * monitoring graph.
     * @param graph The graph
     * @param target The target directory
     * @return The Path object
     */
    def generateDotFile(Graph<Node> graph, File target) {
        val file = new File(target, "configuration.dot")
        target.mkdirs
        file.write(graph.asDotSpecification)
    }

    /**
     * Creates a file containing a CXL specification based on the given
     * monitoring graph.
     * @param graph The graph
     * @param target The target directory
     * @return The Path object
     */
    def generateCxlFile(Graph<Node> graph, File target) {
        val file = new File(target, "configuration.cxl")
        target.mkdirs
        file.write(graph.asCxlSpecification)
    }

    /**
     * Writes a file with the given content.
     * @param file The file to write
     * @param content The file content
     * @return The Path object
     * @throws IOException If there is a problem writing the file
     */
    def private write(File file, CharSequence content) throws IOException {
        Files.write(
            Paths.get(file.toURI),
            content.toString.bytes,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }

    /**
     * Creates a DOT specification based on the given monitoring graph.
     * @param graph The graph
     * @return  The DOT specification
     */
    def asDotSpecification(Graph<Node> graph) '''
        digraph {
            edge [fontname="Arial"];
            node [fontname="Arial", shape=none, fillcolor="grey94", style="rounded, filled", margin=0.1];
            «FOR node : graph.nodes»
                "«node.name»";
            «ENDFOR»
            «FOR node : graph.nodes.filter[!it.dependencies.empty]»
                «FOR input : node.getParameters(true).filter(Input).filter[it.hasSource]»
                    "«node.name»" -> "«input.source.name»" [label="«input.name»"];
                «ENDFOR»
            «ENDFOR»
        }
    '''

    /**
     * Creates a CXL specification (Cmap) based on the given monitoring graph.
     * @param graph The graph
     * @return  The CXL specification
     */
    def asCxlSpecification(Graph<Node> graph) '''
        <?xml version="1.0" encoding="UTF-8"?>
        <cmap xmlns="http://cmap.ihmc.us/xml/cmap/">
            <map>
                <concept-list>
                    «FOR node : graph.nodes»
                        <concept id="«node.name»" label="«node.name»"/>
                    «ENDFOR»
                </concept-list>
                <linking-phrase-list>
                    «val labels = newArrayList»
                    «FOR node : graph.nodes.filter[!it.dependencies.empty]»
                        «FOR input : node.getParameters(true).filter(Input).filter[it.hasSource]»
                            «val label = if (input.name.equals(input.value)) input.name else '''«input.name»/«input.value»'''»
                            «IF !labels.contains(label)»
                                «val dummy = labels.add(label)»
                                <linking-phrase id="«label»" label="«label»"/>
                            «ENDIF»
                        «ENDFOR»
                    «ENDFOR»
                </linking-phrase-list>
                <connection-list>
                    «FOR node : graph.nodes.filter[!it.dependencies.empty]»
                        «FOR input : node.getParameters(true).filter(Input).filter[it.hasSource]»
                            «val label = if (input.name.equals(input.value)) input.name else '''«input.name»/«input.value»'''»
                            <connection id="«UUID.randomUUID()»" from-id="«node.name»" to-id="«label»"/>
                            <connection id="«UUID.randomUUID()»" from-id="«label»" to-id="«input.source.name»" arrowhead="yes"/>
                        «ENDFOR»
                    «ENDFOR»
                </connection-list>
            </map>
        </cmap>
    '''

}
