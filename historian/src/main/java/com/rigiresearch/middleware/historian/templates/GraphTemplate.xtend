package com.rigiresearch.middleware.historian.templates

import com.rigiresearch.middleware.graph.Graph
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.io.IOException
import java.nio.file.StandardOpenOption

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
     * @param graph
     * @param target
     * @return The Path object
     */
    def generateFile(Graph<Graph.Node> graph, File target) {
        val file = new File(target, "configuration.dot")
        target.mkdirs
        file.write(graph.asDotSpecification)
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
    def asDotSpecification(Graph<Graph.Node> graph) {
        val templateBased = graph.nodes.filter[it.templateBased]
        val withoutDependents = graph.nodes.filter[graph.dependents(it).empty]
        val withDependencies = graph.nodes.reject [
            templateBased.toList.indexOf(it) > -1 ||
                withoutDependents.toList.indexOf(it) > -1
        ].filter[!it.getParameters(true).filter(Graph.Input).filter[it.hasSource].empty]
        '''
        digraph {
            edge [fontname="Helvetica Neue", arrowhead="dot", style="dotted"];
            node [shape = box, style="rounded, filled", fillcolor="aliceblue", fontname="Helvetica Neue", margin=0.2];
            «FOR node : templateBased»
                «node.name»;
            «ENDFOR»

            «IF !withDependencies.empty»
                edge [arrowhead="normal", style="normal"]
                node [fillcolor="white"];
                «FOR node : withDependencies»
                    «FOR input : node.getParameters(true).filter(Graph.Input)»
                        «node.name» -> «input.source.name» [label=«input.value»];
                    «ENDFOR»
                «ENDFOR»

            «ENDIF»
            «IF !withoutDependents.empty»
                node [fillcolor="gray94", style="rounded, filled"];
                «FOR node : withoutDependents»
                    «node.name»;
                «ENDFOR»
            «ENDIF»
        }
        '''
    }

}
