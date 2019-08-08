package com.rigiresearch.middleware.graph;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link Graph}.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
final class GraphTest {

    @Test
    void testEmptyGraph() {
        final Graph<Graph.Node> graph = new Graph<>();
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> graph.dependents(new Graph.Node())
        );
        Assertions.assertEquals(
            Collections.emptySet(),
            graph.getNodes(),
            "The graph should not contain any nodes"
        );
    }

    @Test
    void testDependencies() {
        // First node
        final Set<Graph.Parameter> fparams = new TreeSet<>();
        final Graph.Output output = new Graph.Output("output1", "value");
        fparams.add(output);
        final Graph.Node first = new Graph.Node("first", Collections.emptySet());
        // Second node
        final Set<Graph.Parameter> sparams = new TreeSet<>();
        final Graph.Input input = new Graph.Input("input1", output.getName(), first);
        sparams.add(input);
        final Graph.Node second = new Graph.Node("second", sparams);
        final Set<Graph.Node> nodes = new TreeSet<>();
        nodes.add(first);
        nodes.add(second);
        final Graph<Graph.Node> graph = new Graph<>(nodes);
        Assertions.assertEquals(
            graph.dependents(first),
            Collections.singleton(second),
            "\"second\" should depend on \"first\""
        );
        Assertions.assertEquals(
            second.dependencies(),
            Collections.singleton(first),
            "\"first\" should be a dependency"
        );
        Assertions.assertTrue(
            input.hasSource(),
            "\"input1\" should be based on \"output1\""
        );
        Assertions.assertFalse(
            output.isMultivalued(),
            "Output should be single-valued"
        );
        Assertions.assertEquals(
            first,
            input.getSource(),
            "Incorrect source node"
        );
    }

}
