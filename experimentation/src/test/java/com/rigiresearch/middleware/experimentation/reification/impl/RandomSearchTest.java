package com.rigiresearch.middleware.experimentation.reification.impl;

import com.rigiresearch.middleware.experimentation.graph.Metric;
import com.rigiresearch.middleware.experimentation.reification.fitness.FitnessResult;
import com.rigiresearch.middleware.experimentation.sources.prometheus.PrometheusMetricSummary;
import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.GraphParser;
import com.rigiresearch.middleware.graph.Node;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link RandomSearch}.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
final class RandomSearchTest {

    /**
     * The name of the bindings resource.
     */
    private static final String BINDINGS = "bindings.xml";

    /**
     * The name of the demo graph resource.
     */
    private static final String RESOURCE = "graph/simple-graph.xml";

    @Test
    void testItGeneratesCorrectRandomSequences() throws JAXBException {
        final int elements = 10;
        final RandomSearch search = new RandomSearch(elements);
        final Graph<Node> graph = new GraphParser()
            .withBindings(RandomSearchTest.BINDINGS)
            .instance(RandomSearchTest.xml());
        final Optional<Metric> latency = graph.getNodes().stream()
            .filter(node -> "latency".equals(node.getName()))
            .map(Metric.class::cast)
            .findFirst();
        assert latency.isPresent();
        final Function<Map<String, Double>, FitnessResult> fitness = data ->
            new FitnessResult(
                0.0,
                PrometheusMetricSummary.builder()
                    .count(1)
                    .max(0.5)
                    .mean(0.5)
                    .min(0.5)
                    .name("something")
                    .std(0.0)
                    .var(0.0)
                    .build()
            );
        final List<Map<String, Double>> results =
            search.explore(latency.get(), graph, fitness);
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
                        .getResourceAsStream(RandomSearchTest.RESOURCE)
                ),
                StandardCharsets.UTF_8)
        )
            .lines()
            .collect(Collectors.joining("\n"));
    }

}
