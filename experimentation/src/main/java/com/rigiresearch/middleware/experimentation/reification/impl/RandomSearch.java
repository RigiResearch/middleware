package com.rigiresearch.middleware.experimentation.reification.impl;

import com.rigiresearch.middleware.experimentation.graph.Metric;
import com.rigiresearch.middleware.experimentation.graph.NodeDependency;
import com.rigiresearch.middleware.experimentation.reification.SolutionSpaceExploration;
import com.rigiresearch.middleware.experimentation.reification.fitness.FitnessResult;
import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.Node;
import com.rigiresearch.middleware.graph.Property;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link SolutionSpaceExploration} that uses random search
 * to explore combinations of values.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class RandomSearch implements SolutionSpaceExploration {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomSearch.class);

    /**
     * The number of elements to generate;
     */
    private final int elements;

    @Override
    public List<Map<String, Double>> explore(final Metric metric,
        final Graph<? extends Node> graph,
        final Function<Map<String, Double>, FitnessResult> fitness) {
        final Map<Node, int[]> sequences =
            this.randomSequences(this.dependencies(metric));
        if (RandomSearch.LOGGER.isDebugEnabled()) {
            sequences.forEach((key, sequence) ->
                RandomSearch.LOGGER.debug(
                    "Sequence({}): {}", key.getName(), Arrays.toString(sequence)
                )
            );
        }
        return this.results(metric, fitness, sequences);
    }

    /**
     * Evaluates the fitness function on the generated sequences.
     * @param metric The metric
     * @param fitness The fitness function
     * @param sequences The generated random sequences
     * @return A non-null, possibly empty list of results
     */
    private List<Map<String, Double>> results(
        final Metric metric,
        final Function<Map<String, Double>, FitnessResult> fitness,
        final Map<Node, int[]> sequences) {
        final List<Map<String, Double>> results = new ArrayList<>(sequences.size());
        for (int tmp = 0; tmp < this.elements; tmp++) {
            final int position = tmp;
            final Map<String, Double> data = sequences.entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        e -> e.getKey().getName(),
                        e -> (double) e.getValue()[position]
                    )
                );
            final FitnessResult result = fitness.apply(data);
            data.put(metric.getName(), result.getSummary().getMean());
            results.add(data);
            RandomSearch.LOGGER.debug(
                "Instance {}: Result: {}, Data: {}", tmp, result, data);
        }
        return results;
    }

    /**
     * Finds dependencies of the given metric.
     * @param metric The metric
     * @return A non-null, possibly empty list
     */
    private List<Node> dependencies(final Metric metric) {
        return metric.getParameters(false)
            .stream()
            .filter(NodeDependency.class::isInstance)
            .map(NodeDependency.class::cast)
            .map(NodeDependency::getNode)
            .collect(Collectors.toList());
    }

    /**
     * Generates a random sequence for a collection of nodes.
     * @param nodes The collection of nodes
     * @return A possibly empty map
     */
    private Map<Node, int[]> randomSequences(
        final Collection<Node> nodes) {
        return nodes.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                node -> this.randomSequence(this.boundaries(node))
            ));
    }

    /**
     * Generates a random sequence of numbers that hold the given set of constraints.
     * @param boundaries The minimum and maximum values that the random numbers can take
     * @return An array of numbers containing {@code elements} elements.
     */
    private int[] randomSequence(final int[] boundaries) {
        final int[] array = new int[this.elements];
        int tmp = 0;
        while (tmp < array.length) {
            array[tmp] = ThreadLocalRandom.current()
                .nextInt(boundaries[0], boundaries[1]);
            tmp++;
        }
        return array;
    }

    /**
     * Finds the upper and lower boundaries for a particular variable.
     * @param node The node of interest
     * @return An array with two elements (min and max)
     */
    private int[] boundaries(final Node node) {
        final Map<String, String> constraints = node.getMetadata()
            .stream()
            .collect(Collectors.toMap(Property::getName, Property::getValue));
        final String minKey = String.format("%s_min", node.getName());
        final int min;
        if (constraints.containsKey(minKey)) {
            min = Integer.parseInt(constraints.get(minKey));
        } else {
            min = Integer.MIN_VALUE;
        }
        final String maxKey = String.format("%s_max", node.getName());
        final int max;
        if (constraints.containsKey(maxKey)) {
            max = Integer.parseInt(constraints.get(maxKey));
        } else {
            max = Integer.MAX_VALUE;
        }
        return new int[]{ min, max };
    }

}
