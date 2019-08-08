package com.rigiresearch.middleware.graph;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.bind.JAXBException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link GraphParser}.
 * @author Miguel Jimenez (miguel@leslumier.es)
 * @version $Id$
 * @since 0.1.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class GraphParserTest {

    /**
     * The expected XML header.
     */
    private static final String HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";

    /**
     * The error message for marshall errors.
     */
    private static final String ERROR = "The unmarshalled graph is incorrect";

    @Test
    void testEmptyGraph() throws JAXBException {
        final Graph<Graph.Node> graph = new Graph<>();
        Assertions.assertEquals(
            new GraphParser()
                .instance(
                    GraphParserTest.HEADER
                        + "<graph xmlns=\"" + Graph.NAMESPACE + "\">\n"
                        + "</graph>"
                ),
            graph,
            GraphParserTest.ERROR
        );
    }

    @Test
    void testEmptyGraphWithFile() throws Exception {
        final Graph<Graph.Node> graph = new Graph<>();
        final Path tmp = Files.createTempFile("graph", ".xml");
        Files.write(
            tmp,
            (GraphParserTest.HEADER
                + "<graph xmlns=\"" + Graph.NAMESPACE + "\">\n"
                + "</graph>").getBytes()
        );
        Assertions.assertEquals(
            new GraphParser()
                .instance(tmp.toFile()),
            graph,
            GraphParserTest.ERROR
        );
    }

    @Test
    void testSimpleGraphWithBindings() throws JAXBException {
        final Set<Graph.Node> nodes = new TreeSet<>();
        nodes.add(new Graph.Node("test", Collections.emptySet()));
        final Graph<Graph.Node> graph = new Graph<>(nodes);
        Assertions.assertEquals(
            new GraphParser()
                .withBindings("bindings.xml")
                .instance(
                    GraphParserTest.HEADER
                        + "<elements xmlns=\"" + Graph.NAMESPACE + "\">\n"
                        + "    <element name=\"test\"/>\n"
                        + "</elements>"
                ),
            graph,
            GraphParserTest.ERROR
        );
    }

    @Test
    void testComplexGraph() throws JAXBException {
        final Graph.Node simple = new Graph.Node("simple", Collections.emptySet());
        final Set<Graph.Parameter> parameters = new TreeSet<>();
        parameters.add(new Graph.Input("input", "value"));
        parameters.add(new Graph.Output("output", "value"));
        final Graph.Node complex = new Graph.Node("complex", parameters);
        final Set<Graph.Node> nodes = new TreeSet<>();
        nodes.add(simple);
        nodes.add(complex);
        final Graph<Graph.Node> graph = new Graph<>(nodes);
        Assertions.assertEquals(
            new GraphParser()
                .instance(
                    GraphParserTest.HEADER
                        + "<graph xmlns=\"" + Graph.NAMESPACE + "\">\n"
                        + "    <node name=\"simple\"/>\n"
                        + "    <node name=\"complex\">\n"
                        + "        <input name=\"input\">value</input>\n"
                        + "        <output name=\"output\" selector=\"value\"/>\n"
                        + "    </node>\n"
                        + "</graph>"
                ),
            graph,
            GraphParserTest.ERROR
        );
    }

}
