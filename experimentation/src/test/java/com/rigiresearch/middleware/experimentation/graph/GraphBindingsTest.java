package com.rigiresearch.middleware.experimentation.graph;

import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.GraphParser;
import com.rigiresearch.middleware.graph.Node;
import com.rigiresearch.middleware.graph.Property;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests parsing a sample graph with the custom bindings.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
final class GraphBindingsTest {

    /**
     * The name of the bindings resource.
     */
    private static final String BINDINGS = "bindings.xml";

    /**
     * The name of the demo graph resource.
     */
    private static final String RESOURCE = "graph/simple-graph.xml";

    @Test
    void testGeneratingADemoGraph() throws JAXBException {
        final Graph<Node> graph = GraphBindingsTest.graph();
        final OutputStream output = new ByteArrayOutputStream();
        new GraphParser()
            .withBindings(GraphBindingsTest.BINDINGS)
            .write(graph, output);
        Assertions.assertEquals(
            GraphBindingsTest.xml(),
            output.toString(),
            "The output XML should be the same as the contents of the file"
        );
    }

    @Test
    void testLoadingADemoGraph() throws JAXBException {
        Assertions.assertEquals(
            GraphBindingsTest.graph(),
            new GraphParser()
                .withBindings(GraphBindingsTest.BINDINGS)
                .instance(GraphBindingsTest.xml()),
            "The loaded graph should be equal to the graph instance"
        );
    }

    /**
     * Loads the demo graph from the resources.
     * @return An XML-formatted graph
     */
    private static String xml() {
        return new BufferedReader(
            new InputStreamReader(
                Objects.requireNonNull(
                    Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(GraphBindingsTest.RESOURCE)
                ),
                StandardCharsets.UTF_8)
        )
            .lines()
            .collect(Collectors.joining("\n"));
    }

    /**
     * Creates a demo graph.
     * @return A non-null {@link Graph} instance
     */
    private static Graph<Node> graph() {
        final Variable cores = new Variable(
            "cores",
            new HashSet<>(
                Arrays.asList(
                    new Property("cores_C1", "cores >= 1"),
                    new Property("cores_C2", "cores <= 6")
                )
            )
        );
        final Variable demand = new Variable(
            "demand",
            Collections.singleton(new Property("demand_C1", "demand > 0"))
        );
        final Function cost = new Function(
            "cost",
            new HashSet<>(
                Arrays.asList(
                    new NodeDependency(cores, "cost-cores"),
                    new NodeDependency(demand, "cost-demand")
                )
            ),
            Collections.singleton(new Property("equation", "1*cores"))
        );
        final Metric latency = new Metric(
            "latency",
            new HashSet<>(
                Arrays.asList(
                    new NodeDependency(cores, "latency-cores"),
                    new NodeDependency(demand, "latency-demand")
                )
            ),
            Collections.singleton(new Property("type", "minimize"))
        );
        return new Graph<>(
            new HashSet<>(Arrays.asList(cores, demand, cost, latency))
        );
    }

}
