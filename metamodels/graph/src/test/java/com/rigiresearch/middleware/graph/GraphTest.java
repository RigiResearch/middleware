package com.rigiresearch.middleware.graph;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.persistence.jaxb.JAXBContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link Graph}.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class GraphTest {

    @Test
    void testEmptyGraph() {
        final Graph<Node> graph = new Graph<>();
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> graph.dependents(new Node())
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
        final Set<Parameter> fparams = new TreeSet<>();
        final Output output = new Output("output1", "value");
        fparams.add(output);
        final Node first = new Node(
            "first",
            Collections.emptySet(),
            Collections.emptySet()
        );
        // Second node
        final Set<Parameter> sparams = new TreeSet<>();
        final Input input = new Input("input1", output.getName(), first);
        sparams.add(input);
        final Node second = new Node("second", sparams, Collections.emptySet());
        final Set<Node> nodes = new TreeSet<>();
        nodes.add(first);
        nodes.add(second);
        final Graph<Node> graph = new Graph<>(nodes);
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

    @Test
    void testMetadata() {
        final Node first =
            new Node("first", Collections.emptySet(), Collections.emptySet());
        final Set<Property> metadata = new TreeSet<>();
        metadata.add(new Property("key", "value"));
        final Node second =
            new Node("second", Collections.emptySet(), metadata);
        final Set<Node> nodes = new TreeSet<>();
        nodes.add(first);
        nodes.add(second);
        final Graph<Node> graph = new Graph<>(nodes);
        Assertions.assertEquals(
            Collections.emptySet(),
            graph.getNodes()
                .stream()
                .filter(tmp -> tmp.equals(first))
                .findAny()
                .get()
                .getMetadata(),
            "The metadata should be empty"
        );
        Assertions.assertEquals(
            1,
            graph.getNodes()
                .stream()
                .filter(tmp -> tmp.equals(second))
                .findAny()
                .get()
                .getMetadata()
                .size(),
            "The metadata should contain one element"
        );
    }

    @Test
    void testSchemaGeneration() throws Exception {
        final Path path = Paths.get("schema/graph.xsd");
        final SchemaOutputResolver resolver = new SchemaOutputResolver() {
            @Override
            public Result createOutput(final String namespace, final String filename)
                throws IOException {
                final StreamResult result = new StreamResult(path.toFile());
                result.setSystemId(path.toUri().toURL().toString());
                return result;
            }
        };
        JAXBContext.newInstance(Graph.class)
            .generateSchema(resolver);
    }

}
